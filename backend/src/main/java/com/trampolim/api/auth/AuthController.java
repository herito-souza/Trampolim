package com.trampolim.api.auth;

import com.trampolim.api.security.JwtUtil;
import com.trampolim.api.user.AppUser;
import com.trampolim.api.user.AppUserRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {
    private final AppUserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final ObjectProvider<JavaMailSender> mailSenders;
    private final JdbcTemplate jdbc;
    @Value("${app.frontend-url:http://localhost:5173}") private String frontendUrl;
    @Value("${app.mail.enabled:false}") private boolean mailEnabled;

    public record Credentials(String email, String password) {}
    public record Registration(String name, String email, String password) {}
    public record CompanyRegistration(String name, String email, String password, String legalName, String tradeName, String cnpj, String phone, String website, String industry, String address, String city, String state) {}
    public record CompanyInviteRegistration(String token, String name, String email, String password) {}
    public record AuthResponse(String token, UserSummary user, boolean emailVerified, String accountType) {}
    public record UserSummary(String name, String email) {}
    public record ResetRequest(String email) {}
    public record ResetPassword(String token, String password) {}
    private final ConcurrentHashMap<String, RateWindow> resetRequestCounts = new ConcurrentHashMap<>();
    private record RateWindow(Instant expiresAt, int count) {}

    @PostMapping("/cadastro")
    public AuthResponse register(@RequestBody Registration request, HttpServletRequest servletRequest) {
        limitRequests("register:" + servletRequest.getRemoteAddr(), 10, 60);
        String name = request.name() == null ? "" : request.name().trim();
        String email = normalizeEmail(request.email());
        String password = request.password() == null ? "" : request.password();
        if (name.isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nome completo: preencha este campo.");
        if (!validEmail(email)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "E-mail: informe um endereço válido.");
        if (password.length() < 8) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Senha: use pelo menos 8 caracteres.");
        if (users.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Este e-mail já está cadastrado.");
        }

        AppUser user = new AppUser();
        user.setFullName(name);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setEmailVerified(!mailEnabled);
        String verificationToken = null;
        JavaMailSender configuredMailSender = null;
        if (mailEnabled) {
            configuredMailSender = mailSenders.getIfAvailable();
            if (configuredMailSender == null) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "O envio de confirmação ainda não está configurado.");
            verificationToken = UUID.randomUUID().toString();
            user.setEmailVerificationTokenHash(hashToken(verificationToken));
            user.setEmailVerificationExpiresAt(Instant.now().plus(24, ChronoUnit.HOURS));
        }
        users.save(user);
        log.info("auth_registration_created userId={} emailConfirmationRequired={}", user.getId(), mailEnabled);
        if (mailEnabled) sendVerificationMail(user, verificationToken, configuredMailSender);
        return responseFor(user);
    }

    @PostMapping("/company/cadastro")
    public AuthResponse registerCompany(@RequestBody CompanyRegistration request, HttpServletRequest servletRequest) {
        limitRequests("company-register:" + servletRequest.getRemoteAddr(), 10, 60);
        String name = clean(request.name()); String email = normalizeEmail(request.email());
        String password = request.password() == null ? "" : request.password();
        String cnpj = request.cnpj() == null ? "" : request.cnpj().replaceAll("[^A-Za-z0-9]", "").toUpperCase();
        if (name.isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nome completo do responsável: preencha este campo.");
        if (!validEmail(email)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "E-mail: informe um endereço válido.");
        if (password.length() < 8) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Senha: use pelo menos 8 caracteres.");
        if (clean(request.legalName()).isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Razão social: preencha este campo.");
        if (clean(request.tradeName()).isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nome fantasia: preencha este campo.");
        if (cnpj.isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CNPJ: preencha este campo para identificar a empresa.");
        if (clean(request.phone()).isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Telefone: preencha este campo.");
        if (clean(request.industry()).isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Setor de atuação: preencha este campo.");
        if (clean(request.address()).isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Endereço: preencha este campo.");
        if (clean(request.city()).isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cidade: preencha este campo.");
        if (request.state() == null || !request.state().matches("(?i)[a-z]{2}")) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "UF: informe a sigla de dois caracteres do estado.");
        if (users.existsByEmail(email)) throw new ResponseStatusException(HttpStatus.CONFLICT, "Este e-mail já está cadastrado.");
        AppUser user = new AppUser(); user.setFullName(name); user.setEmail(email); user.setPasswordHash(passwordEncoder.encode(password));
        user.setAccountType("COMPANY"); user.setEmailVerified(true); users.save(user);
        try {
            Long companyId = jdbc.queryForObject("INSERT INTO companies(user_id, legal_name, trade_name, cnpj, phone, website, industry, address, city, state) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id",
                    Long.class, user.getId(), request.legalName().trim(), request.tradeName().trim(), cnpj, request.phone().trim(), clean(request.website()), request.industry().trim(), request.address().trim(), request.city().trim(), request.state().toUpperCase());
            jdbc.update("INSERT INTO company_members(company_id, user_id, role) VALUES (?, ?, 'OWNER')", companyId, user.getId());
        } catch (org.springframework.dao.DuplicateKeyException exception) {
            users.delete(user);
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Este CNPJ já possui uma conta empresarial.");
        }
        return responseFor(user);
    }

    @PostMapping("/company/join")
    @Transactional
    public AuthResponse joinCompany(@RequestBody CompanyInviteRegistration request, HttpServletRequest servletRequest) {
        limitRequests("company-join:" + servletRequest.getRemoteAddr(), 10, 60);
        String name = clean(request.name()), email = normalizeEmail(request.email());
        String password = request.password() == null ? "" : request.password();
        String tokenHash = hashToken(clean(request.token()));
        if (name.isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nome completo: preencha este campo.");
        if (!validEmail(email)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "E-mail: informe um endereço válido.");
        if (password.length() < 8) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Senha: use pelo menos 8 caracteres.");
        var invites = jdbc.query("SELECT id, company_id, email, role FROM company_invites WHERE token_hash=? AND accepted_at IS NULL AND expires_at > CURRENT_TIMESTAMP", (rs, row) -> new Object[]{rs.getLong(1), rs.getLong(2), rs.getString(3), rs.getString(4)}, tokenHash);
        if (invites.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Este convite expirou ou já foi utilizado. Solicite um novo à empresa.");
        Object[] invite = invites.get(0);
        if (!email.equals(invite[2])) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Use o e-mail informado no convite da empresa.");
        if (users.existsByEmail(email)) throw new ResponseStatusException(HttpStatus.CONFLICT, "Este e-mail já possui uma conta. Entre nela para aceitar um convite existente.");
        AppUser user = new AppUser(); user.setFullName(name); user.setEmail(email); user.setPasswordHash(passwordEncoder.encode(password)); user.setAccountType("COMPANY"); user.setEmailVerified(true); users.save(user);
        jdbc.update("INSERT INTO company_members(company_id, user_id, role) VALUES (?, ?, ?)", invite[1], user.getId(), invite[3]);
        jdbc.update("UPDATE company_invites SET accepted_at=CURRENT_TIMESTAMP WHERE id=?", invite[0]);
        return responseFor(user);
    }

    @PostMapping("/email/resend")
    public Map<String, String> resendVerification(@RequestBody ResetRequest request, HttpServletRequest servletRequest) {
        limitResetRequests("verify:" + servletRequest.getRemoteAddr());
        if (!mailEnabled || mailSenders.getIfAvailable() == null) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "O envio de confirmação ainda não está configurado.");
        users.findByEmail(normalizeEmail(request.email())).filter(user -> !user.isEmailVerified()).ifPresent(user -> {
            String token = UUID.randomUUID().toString(); user.setEmailVerificationTokenHash(hashToken(token));
            user.setEmailVerificationExpiresAt(Instant.now().plus(24, ChronoUnit.HOURS)); users.save(user);
            sendVerificationMail(user, token, mailSenders.getIfAvailable());
        });
        return Map.of("message", "Se a conta precisar de confirmação, enviaremos um novo link para o endereço informado.");
    }

    @PostMapping("/login")
    public AuthResponse login(@RequestBody Credentials request, HttpServletRequest servletRequest) {
        limitRequests("login:" + servletRequest.getRemoteAddr(), 10, 15);
        String email = normalizeEmail(request.email());
        AppUser user = users.findByEmail(email).orElse(null);
        if (user == null || request.password() == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            if (user != null) {
                int attempts = user.getFailedLoginAttempts() + 1;
                user.setFailedLoginAttempts(attempts);
                if (attempts >= 5) { user.setLoginLockedUntil(Instant.now().plus(15, ChronoUnit.MINUTES)); user.setFailedLoginAttempts(0); log.warn("auth_account_locked userId={} durationMinutes=15", user.getId()); }
                users.save(user);
            }
            log.warn("auth_login_failed knownAccount={}", user != null);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "E-mail ou senha incorretos. Confira os dois campos e tente novamente.");
        }
        if (user.getLoginLockedUntil() != null && user.getLoginLockedUntil().isAfter(Instant.now())) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Muitas tentativas. Aguarde 15 minutos antes de tentar novamente.");
        }
        if (!user.isEmailVerified()) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Confirme seu e-mail pelo link enviado antes de entrar.");
        user.setFailedLoginAttempts(0); user.setLoginLockedUntil(null); users.save(user);
        log.info("auth_login_success userId={}", user.getId());
        return responseFor(user);
    }

    @GetMapping("/email/verify")
    public Map<String, String> verifyEmail(@org.springframework.web.bind.annotation.RequestParam String token) {
        AppUser user = users.findByEmailVerificationTokenHash(hashToken(token))
                .filter(found -> found.getEmailVerificationExpiresAt() != null && found.getEmailVerificationExpiresAt().isAfter(Instant.now()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "O link de confirmação expirou ou já foi usado."));
        user.setEmailVerified(true); user.setEmailVerificationTokenHash(null); user.setEmailVerificationExpiresAt(null);
        users.save(user);
        return Map.of("message", "E-mail confirmado. Agora você já pode entrar na sua conta.");
    }

    @PostMapping("/password/reset-request")
    public Map<String, String> requestPasswordReset(@RequestBody ResetRequest request, HttpServletRequest servletRequest) {
        limitResetRequests(servletRequest.getRemoteAddr());
        JavaMailSender mailSender = mailSenders.getIfAvailable();
        if (!mailEnabled || mailSender == null) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "O envio de e-mail ainda não foi configurado neste servidor.");
        String email = normalizeEmail(request.email());
        users.findByEmail(email).ifPresent(user -> {
            String token = UUID.randomUUID().toString();
            user.setPasswordResetTokenHash(hashToken(token));
            user.setPasswordResetExpiresAt(Instant.now().plus(30, ChronoUnit.MINUTES));
            users.save(user);
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(user.getEmail());
            message.setSubject("Redefina sua senha do Trampolim");
            message.setText("Use este link em até 30 minutos para criar uma nova senha: " + frontendUrl + "/?resetToken=" + token);
            mailSender.send(message);
        });
        return Map.of("message", "Se o e-mail estiver cadastrado, você receberá instruções para redefinir sua senha.");
    }

    @PostMapping("/password/reset")
    public Map<String, String> resetPassword(@RequestBody ResetPassword request) {
        String token = request.token() == null ? "" : request.token().trim();
        String password = request.password() == null ? "" : request.password();
        if (password.length() < 8) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A senha deve ter pelo menos 8 caracteres.");
        AppUser user = users.findByPasswordResetTokenHash(hashToken(token))
                .filter(found -> found.getPasswordResetExpiresAt() != null && found.getPasswordResetExpiresAt().isAfter(Instant.now()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "O link expirou ou já foi utilizado. Solicite outro."));
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setTokenVersion(user.getTokenVersion() + 1);
        user.setFailedLoginAttempts(0); user.setLoginLockedUntil(null);
        user.setPasswordResetTokenHash(null);
        user.setPasswordResetExpiresAt(null);
        users.save(user);
        log.info("auth_password_changed userId={} sessionsRevoked=true", user.getId());
        return Map.of("message", "Senha alterada. Você já pode entrar na sua conta.");
    }

    private AuthResponse responseFor(AppUser user) {
        String token = user.isEmailVerified() ? jwtUtil.generateToken(user.getEmail(), user.getTokenVersion()) : "";
        String accountType = user.getAccountType();
        Integer memberships = jdbc.queryForObject("SELECT COUNT(*) FROM company_members WHERE user_id=?", Integer.class, user.getId());
        if (memberships != null && memberships > 0) accountType = "COMPANY";
        return new AuthResponse(token, new UserSummary(user.getFullName(), user.getEmail()), user.isEmailVerified(), accountType);
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    private String clean(String value) { return value == null ? "" : value.trim(); }
    private boolean validEmail(String email) { return email != null && email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"); }

    private String hashToken(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private void limitResetRequests(String remoteAddress) {
        limitRequests("reset:" + remoteAddress, 5, 60);
    }

    private void limitRequests(String key, int maximum, int windowMinutes) {
        Instant now = Instant.now();
        RateWindow current = resetRequestCounts.compute(key, (bucket, previous) -> previous == null || previous.expiresAt().isBefore(now)
                ? new RateWindow(now.plus(windowMinutes, ChronoUnit.MINUTES), 1) : new RateWindow(previous.expiresAt(), previous.count() + 1));
        if (resetRequestCounts.size() > 10_000) resetRequestCounts.entrySet().removeIf(entry -> entry.getValue().expiresAt().isBefore(now));
        if (current.count() > maximum) throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Muitas solicitações. Tente novamente mais tarde.");
    }

    private void sendVerificationMail(AppUser user, String token, JavaMailSender sender) {
        SimpleMailMessage message = new SimpleMailMessage(); message.setTo(user.getEmail());
        message.setSubject("Confirme seu e-mail no Trampolim");
        message.setText("Confirme seu e-mail em até 24 horas: " + frontendUrl + "/?verifyToken=" + token);
        sender.send(message);
    }
}
