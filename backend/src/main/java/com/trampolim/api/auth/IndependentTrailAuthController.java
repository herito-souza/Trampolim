package com.trampolim.api.auth;

import com.trampolim.api.security.TrailTokenService;
import com.trampolim.api.security.TrailTokenService.Trail;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** Account endpoints for credentials stored only in the Empresas and Capacita databases. */
@RestController
@RequestMapping("/api")
@Slf4j
public class IndependentTrailAuthController {
    private final ObjectProvider<JdbcTemplate> empresasJdbc;
    private final ObjectProvider<JdbcTemplate> capacitaJdbc;
    private final PasswordEncoder passwordEncoder;
    private final TrailTokenService tokens;
    private final ObjectProvider<JavaMailSender> mailSenders;
    @Value("${app.frontend-url:http://localhost:5173}") private String frontendUrl;
    @Value("${app.mail.enabled:false}") private boolean mailEnabled;
    private final com.trampolim.api.security.BoundedRateLimiter rateLimits = new com.trampolim.api.security.BoundedRateLimiter();

    public IndependentTrailAuthController(
            @Qualifier("empresasJdbcTemplate") ObjectProvider<JdbcTemplate> empresasJdbc,
            @Qualifier("capacitaJdbcTemplate") ObjectProvider<JdbcTemplate> capacitaJdbc,
            PasswordEncoder passwordEncoder,
            TrailTokenService tokens,
            ObjectProvider<JavaMailSender> mailSenders) {
        this.empresasJdbc = empresasJdbc;
        this.capacitaJdbc = capacitaJdbc;
        this.passwordEncoder = passwordEncoder;
        this.tokens = tokens;
        this.mailSenders = mailSenders;
    }

    public record Registration(String name, String email, String password, String termsAccepted) {}
    public record StudentRegistration(String name, String email, String password, String educationStage,
            String institutionName, boolean termsAccepted, String ageBand) {}
    public record CompanyRegistration(String name, String email, String password, String legalName,
            String tradeName, String cnpj, String phone, String website, String industry, String address,
            String city, String state, boolean termsAccepted) {}
    public record CompanyInviteRegistration(String token, String name, String email, String password,
            boolean termsAccepted) {}
    public record OrganizationInviteRegistration(String token, String name, String email, String password,
            boolean termsAccepted) {}
    public record Credentials(String email, String password) {}
    public record PasswordResetRequest(String email) {}
    public record PasswordReset(String token, String password) {}
    public record AuthResponse(String token, UserSummary user, boolean emailVerified, String accountType,
            String accountStatus) {}
    public record UserSummary(String name, String email) {}

    @PostMapping("/capacita/auth/register")
    public AuthResponse registerStudent(@RequestBody StudentRegistration request,
            jakarta.servlet.http.HttpServletRequest http) {
        limit("capacita-register:" + http.getRemoteAddr(), 8, 60);
        JdbcTemplate jdbc = database(capacitaJdbc, "Capacita");
        requireMailConfigured();
        String name = clean(request.name());
        String email = email(request.email());
        String password = request.password() == null ? "" : request.password();
        String stage = clean(request.educationStage()).toUpperCase(Locale.ROOT);
        if (name.isBlank() || name.length() > 120) bad("Informe seu nome completo (até 120 caracteres).");
        validateEmailAndPassword(email, password);
        if (!List.of("HIGH_SCHOOL", "UNIVERSITY", "CAREER_START").contains(stage)) bad("Selecione uma etapa de ensino válida.");
        if (!List.of("AGE_14_17", "AGE_18_PLUS").contains(clean(request.ageBand()))) bad("O cadastro do Capacita atende pessoas a partir de 14 anos. Selecione sua faixa etária.");
        if (!request.termsAccepted()) bad("Aceite os termos para criar sua conta educacional.");
        if (jdbc.queryForObject("SELECT COUNT(*) FROM student_users WHERE email=?", Integer.class, email) > 0)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Este e-mail já possui uma conta no Capacita.");
        Long id = jdbc.queryForObject("INSERT INTO student_users(email,password_hash,full_name,education_stage,institution_name,email_verified,account_status,terms_accepted_at,age_band,age_declared_at) VALUES (?,?,?,?,?,?,'ACTIVE',CURRENT_TIMESTAMP,?,CURRENT_TIMESTAMP) RETURNING id",
                Long.class, email, passwordEncoder.encode(password), name, stage, limit(clean(request.institutionName()), 180), !mailEnabled, clean(request.ageBand()));
        if (mailEnabled) verifyEmailLater(jdbc, "student_users", id, email, "Capacita");
        return response(Trail.CAPACITA, id, email, name, !mailEnabled, 0, "ACTIVE");
    }

    /** Creates an independent Empresas account for an invited association or education organization member. */
    @PostMapping("/empresas/auth/organization-invite/register")
    @org.springframework.transaction.annotation.Transactional(transactionManager = "empresasTransactionManager")
    public AuthResponse registerOrganizationMember(@RequestBody OrganizationInviteRegistration request,
            jakarta.servlet.http.HttpServletRequest http) {
        limit("empresas-organization-invite:" + http.getRemoteAddr(), 8, 60);
        JdbcTemplate jdbc = database(empresasJdbc, "Empresas");
        requireMailConfigured();
        String email = email(request == null ? null : request.email());
        String name = clean(request == null ? null : request.name());
        String password = request == null || request.password() == null ? "" : request.password();
        String inviteToken = clean(request == null ? null : request.token());
        if (name.isBlank() || name.length() > 120) bad("Informe seu nome completo.");
        validateEmailAndPassword(email, password);
        if (request == null || !request.termsAccepted()) bad("Aceite os termos para criar sua conta empresarial.");
        if (inviteToken.isBlank()) bad("O convite institucional é inválido.");
        List<Long> invitations = jdbc.query("SELECT i.id FROM enterprise_organization_invites i JOIN enterprise_organizations o ON o.id=i.organization_id AND o.status='ACTIVE' WHERE i.token_hash=? AND LOWER(i.email)=? AND i.accepted_at IS NULL AND i.expires_at>CURRENT_TIMESTAMP FOR UPDATE OF i",
                (rs, row) -> rs.getLong(1), hash(inviteToken), email);
        if (invitations.isEmpty()) bad("O convite expirou, já foi usado ou pertence a outro e-mail.");
        if (jdbc.queryForObject("SELECT COUNT(*) FROM company_users WHERE LOWER(email)=LOWER(?)", Integer.class, email) > 0)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Este e-mail já possui uma conta em Empresas.");

        Long userId = jdbc.queryForObject("INSERT INTO company_users(email,password_hash,full_name,email_verified,account_status,terms_accepted_at) VALUES (?,?,?,?,'ACTIVE',CURRENT_TIMESTAMP) RETURNING id",
                Long.class, email, passwordEncoder.encode(password), name, !mailEnabled);
        jdbc.update("INSERT INTO enterprise_organization_members(organization_id,email,full_name,role) SELECT organization_id,LOWER(email),?,role FROM enterprise_organization_invites WHERE id=? ON CONFLICT DO NOTHING",
                name, invitations.get(0));
        jdbc.update("UPDATE enterprise_organization_invites SET accepted_at=CURRENT_TIMESTAMP WHERE id=?", invitations.get(0));
        if (mailEnabled) verifyEmailLater(jdbc, "company_users", userId, email, "Empresas");
        return response(Trail.EMPRESAS, userId, email, name, !mailEnabled, 0, "ACTIVE");
    }

    @PostMapping("/capacita/auth/login")
    public AuthResponse loginStudent(@RequestBody Credentials request, jakarta.servlet.http.HttpServletRequest http) {
        limit("capacita-login:" + http.getRemoteAddr(), 12, 15);
        JdbcTemplate jdbc = database(capacitaJdbc, "Capacita");
        String email = email(request.email());
        List<Object[]> rows = jdbc.query("SELECT id,full_name,password_hash,email_verified,account_status,token_version,failed_login_attempts,login_locked_until FROM student_users WHERE email=?",
                (rs, row) -> new Object[]{rs.getLong(1), rs.getString(2), rs.getString(3), rs.getBoolean(4), rs.getString(5), rs.getInt(6), rs.getInt(7), rs.getTimestamp(8)}, email);
        if (rows.isEmpty()) unauthorized();
        Object[] user = rows.get(0);
        if (user[7] != null && ((java.sql.Timestamp) user[7]).toInstant().isAfter(Instant.now()))
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Muitas tentativas. Aguarde antes de tentar novamente.");
        if (request.password() == null || !passwordEncoder.matches(request.password(), (String) user[2])) {
            jdbc.update("UPDATE student_users SET failed_login_attempts=CASE WHEN failed_login_attempts+1>=5 THEN 0 ELSE failed_login_attempts+1 END,login_locked_until=CASE WHEN failed_login_attempts+1>=5 THEN CURRENT_TIMESTAMP+INTERVAL '15 minutes' ELSE login_locked_until END WHERE id=?",user[0]);
            unauthorized();
        }
        if (!"ACTIVE".equals(user[4])) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Esta conta educacional está indisponível.");
        if (!(Boolean) user[3]) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Confirme seu e-mail antes de entrar.");
        jdbc.update("UPDATE student_users SET failed_login_attempts=0,login_locked_until=NULL,last_login_at=CURRENT_TIMESTAMP WHERE id=?", user[0]);
        return response(Trail.CAPACITA, (Long) user[0], email, (String) user[1], true, (Integer) user[5], (String) user[4]);
    }

    @PostMapping("/empresas/auth/register")
    @org.springframework.transaction.annotation.Transactional(transactionManager = "empresasTransactionManager")
    public AuthResponse registerCompany(@RequestBody CompanyRegistration request,
            jakarta.servlet.http.HttpServletRequest http) {
        limit("empresas-register:" + http.getRemoteAddr(), 8, 60);
        JdbcTemplate jdbc = database(empresasJdbc, "Empresas");
        requireMailConfigured();
        String name = clean(request.name());
        String email = email(request.email());
        String password = request.password() == null ? "" : request.password();
        String cnpj = clean(request.cnpj()).replaceAll("[^A-Za-z0-9]", "").toUpperCase(Locale.ROOT);
        if (name.isBlank() || name.length() > 120) bad("Informe o nome do responsável.");
        validateEmailAndPassword(email, password);
        if (!request.termsAccepted()) bad("Aceite os termos para criar sua conta empresarial.");
        if (clean(request.legalName()).isBlank() || clean(request.tradeName()).isBlank() || cnpj.isBlank()
                || clean(request.phone()).isBlank() || clean(request.industry()).isBlank()
                || clean(request.address()).isBlank() || clean(request.city()).isBlank()
                || request.state() == null || !request.state().matches("(?i)[a-z]{2}"))
            bad("Preencha os dados obrigatórios da empresa e do responsável.");
        if (!cnpj.matches("[0-9]{14}")) bad("Informe um CNPJ com 14 dígitos.");
        List<Object[]> pendingLegacyClaims = jdbc.query("""
                SELECT u.id,u.token_version FROM company_users u
                JOIN companies c ON c.owner_user_id=u.id
                WHERE LOWER(u.email)=LOWER(?) AND c.cnpj=? AND c.legacy_company_id IS NOT NULL
                  AND u.account_status='PENDING' AND u.email_verified=FALSE
                """, (rs, row) -> new Object[]{rs.getLong(1), rs.getInt(2)}, email, cnpj);
        if (!pendingLegacyClaims.isEmpty())
            return claimMigratedCompany(jdbc, pendingLegacyClaims.get(0), email, name, password);
        if (jdbc.queryForObject("SELECT COUNT(*) FROM company_users WHERE LOWER(email)=LOWER(?)", Integer.class, email) > 0)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Este e-mail já possui uma conta em Empresas ou os dados não correspondem ao cadastro legado.");
        if (jdbc.queryForObject("SELECT COUNT(*) FROM companies WHERE cnpj=?", Integer.class, cnpj) > 0)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Este CNPJ já possui uma organização cadastrada ou os dados não correspondem ao cadastro legado.");
        String encoded = passwordEncoder.encode(password);
        try {
            Long id = jdbc.queryForObject("WITH account AS (INSERT INTO company_users(email,password_hash,full_name,email_verified,account_status,terms_accepted_at) VALUES (?,?,?,?,'ACTIVE',CURRENT_TIMESTAMP) RETURNING id,email,full_name), organization AS (INSERT INTO companies(owner_user_id,legal_name,trade_name,cnpj,phone,website,industry,address,city,state) SELECT id,?,?,?,?,?,?,?,?,? FROM account RETURNING id,owner_user_id), membership AS (INSERT INTO company_members(company_id,user_id,role) SELECT id,owner_user_id,'OWNER' FROM organization RETURNING user_id) SELECT account.id FROM account JOIN membership ON membership.user_id=account.id",
                    Long.class, email, encoded, name, !mailEnabled, clean(request.legalName()), clean(request.tradeName()), cnpj,
                    clean(request.phone()), clean(request.website()), clean(request.industry()), clean(request.address()), clean(request.city()), request.state().toUpperCase(Locale.ROOT));
            if (mailEnabled) verifyEmailLater(jdbc, "company_users", id, email, "Empresas");
            return response(Trail.EMPRESAS, id, email, name, !mailEnabled, 0, "ACTIVE");
        } catch (org.springframework.dao.DuplicateKeyException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Este e-mail ou CNPJ já possui conta empresarial.");
        }
    }

    private AuthResponse claimMigratedCompany(JdbcTemplate jdbc, Object[] account, String email, String name, String password) {
        long id = (Long) account[0];
        int version = (Integer) account[1] + 1;
        int updated = jdbc.update("""
                UPDATE company_users SET full_name=?,password_hash=?,terms_accepted_at=CURRENT_TIMESTAMP,
                    email_verified=?,account_status='ACTIVE',token_version=?,failed_login_attempts=0,
                    login_locked_until=NULL WHERE id=? AND account_status='PENDING' AND email_verified=FALSE
                """, name, passwordEncoder.encode(password), !mailEnabled, version, id);
        if (updated != 1) throw new ResponseStatusException(HttpStatus.CONFLICT, "Não foi possível concluir a recuperação deste cadastro empresarial.");
        if (mailEnabled) verifyEmailLater(jdbc, "company_users", id, email, "Empresas");
        return response(Trail.EMPRESAS, id, email, name, !mailEnabled, version, "ACTIVE");
    }

    @PostMapping("/empresas/auth/login")
    public AuthResponse loginCompany(@RequestBody Credentials request, jakarta.servlet.http.HttpServletRequest http) {
        limit("empresas-login:" + http.getRemoteAddr(), 12, 15);
        JdbcTemplate jdbc = database(empresasJdbc, "Empresas");
        String email = email(request.email());
        List<Object[]> rows = jdbc.query("SELECT id,full_name,password_hash,email_verified,account_status,token_version,failed_login_attempts,login_locked_until FROM company_users WHERE email=?",
                (rs, row) -> new Object[]{rs.getLong(1), rs.getString(2), rs.getString(3), rs.getBoolean(4), rs.getString(5), rs.getInt(6), rs.getInt(7), rs.getTimestamp(8)}, email);
        if (rows.isEmpty()) unauthorized();
        Object[] user = rows.get(0);
        if (user[7] != null && ((java.sql.Timestamp) user[7]).toInstant().isAfter(Instant.now()))
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Muitas tentativas. Aguarde antes de tentar novamente.");
        if (request.password() == null || !passwordEncoder.matches(request.password(), (String) user[2])) {
            jdbc.update("UPDATE company_users SET failed_login_attempts=CASE WHEN failed_login_attempts+1>=5 THEN 0 ELSE failed_login_attempts+1 END,login_locked_until=CASE WHEN failed_login_attempts+1>=5 THEN CURRENT_TIMESTAMP+INTERVAL '15 minutes' ELSE login_locked_until END WHERE id=?",user[0]);
            unauthorized();
        }
        if (!"ACTIVE".equals(user[4])) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Esta conta empresarial está indisponível.");
        if (!(Boolean) user[3]) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Confirme seu e-mail antes de entrar.");
        jdbc.update("UPDATE company_users SET failed_login_attempts=0,login_locked_until=NULL,last_login_at=CURRENT_TIMESTAMP WHERE id=?", user[0]);
        return response(Trail.EMPRESAS, (Long) user[0], email, (String) user[1], true, (Integer) user[5], (String) user[4]);
    }

    @PostMapping("/empresas/auth/join")
    public AuthResponse joinCompany(@RequestBody CompanyInviteRegistration request,
            jakarta.servlet.http.HttpServletRequest http) {
        limit("empresas-join:" + http.getRemoteAddr(), 8, 60);
        JdbcTemplate jdbc = database(empresasJdbc, "Empresas");
        requireMailConfigured();
        String name = clean(request.name());
        String email = email(request.email());
        String password = request.password() == null ? "" : request.password();
        String inviteToken = clean(request.token());
        if (!request.termsAccepted()) bad("Aceite os termos para criar sua conta empresarial.");
        if (name.isBlank() || name.length() > 120) bad("Informe o nome completo.");
        validateEmailAndPassword(email, password);
        if (inviteToken.isBlank()) bad("O convite empresarial é inválido.");
        String verification = mailEnabled ? UUID.randomUUID().toString() : null;
        try {
            Long id = jdbc.queryForObject("WITH invite AS (SELECT id,company_id,email,role FROM company_invites WHERE token_hash=? AND LOWER(email)=? AND accepted_at IS NULL AND expires_at>CURRENT_TIMESTAMP FOR UPDATE), account AS (INSERT INTO company_users(email,password_hash,full_name,email_verified,account_status,terms_accepted_at) SELECT ?,?,?,?,'ACTIVE',CURRENT_TIMESTAMP FROM invite RETURNING id,email,full_name), membership AS (INSERT INTO company_members(company_id,user_id,role) SELECT invite.company_id,account.id,invite.role FROM invite CROSS JOIN account RETURNING user_id), accepted AS (UPDATE company_invites SET accepted_at=CURRENT_TIMESTAMP WHERE id IN (SELECT id FROM invite) AND EXISTS (SELECT 1 FROM membership) RETURNING id) SELECT account.id FROM account JOIN accepted ON TRUE",
                    Long.class, hash(inviteToken), email, email, passwordEncoder.encode(password), name, !mailEnabled);
            if (mailEnabled) verifyEmailLater(jdbc, "company_users", id, email, "Empresas");
            return response(Trail.EMPRESAS, id, email, name, !mailEnabled, 0, "ACTIVE");
        } catch (org.springframework.dao.EmptyResultDataAccessException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O convite expirou, já foi usado ou pertence a outro e-mail.");
        } catch (org.springframework.dao.DuplicateKeyException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Este e-mail já possui uma conta em Empresas.");
        }
    }

    @PostMapping("/capacita/auth/password/reset-request")
    public Map<String, String> resetStudentRequest(@RequestBody PasswordResetRequest request,
            jakarta.servlet.http.HttpServletRequest http) {
        return requestReset(database(capacitaJdbc, "Capacita"), "student_users", "Capacita", email(request.email()), http);
    }

    @PostMapping("/empresas/auth/password/reset-request")
    public Map<String, String> resetCompanyRequest(@RequestBody PasswordResetRequest request,
            jakarta.servlet.http.HttpServletRequest http) {
        return requestReset(database(empresasJdbc, "Empresas"), "company_users", "Empresas", email(request.email()), http);
    }

    @PostMapping("/capacita/auth/password/reset")
    public Map<String, String> resetStudent(@RequestBody PasswordReset request) {
        return resetPassword(database(capacitaJdbc, "Capacita"), "student_users", request);
    }

    @PostMapping("/empresas/auth/password/reset")
    public Map<String, String> resetCompany(@RequestBody PasswordReset request) {
        return resetPassword(database(empresasJdbc, "Empresas"), "company_users", request);
    }

    @org.springframework.web.bind.annotation.GetMapping("/capacita/auth/email/verify")
    public Map<String, String> verifyStudentEmail(@org.springframework.web.bind.annotation.RequestParam String token) {
        return verifyEmail(database(capacitaJdbc, "Capacita"), "student_users", token);
    }

    @org.springframework.web.bind.annotation.GetMapping("/empresas/auth/email/verify")
    public Map<String, String> verifyCompanyEmail(@org.springframework.web.bind.annotation.RequestParam String token) {
        return verifyEmail(database(empresasJdbc, "Empresas"), "company_users", token);
    }

    @PostMapping("/capacita/auth/email/resend")
    public Map<String, String> resendStudentEmail(@RequestBody PasswordResetRequest request) {
        return resendVerification(database(capacitaJdbc, "Capacita"), "student_users", "Capacita", email(request.email()));
    }

    @PostMapping("/empresas/auth/email/resend")
    public Map<String, String> resendCompanyEmail(@RequestBody PasswordResetRequest request) {
        return resendVerification(database(empresasJdbc, "Empresas"), "company_users", "Empresas", email(request.email()));
    }

    @PostMapping("/empresas/session/logout")
    @org.springframework.web.bind.annotation.ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    public void logoutCompany(@org.springframework.security.core.annotation.AuthenticationPrincipal String email,
            jakarta.servlet.http.HttpServletRequest request) {
        revokeSession(Trail.EMPRESAS, database(empresasJdbc, "Empresas"), "company_users", email, request);
    }

    @PostMapping("/capacita/session/logout")
    @org.springframework.web.bind.annotation.ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    public void logoutStudent(@org.springframework.security.core.annotation.AuthenticationPrincipal String email,
            jakarta.servlet.http.HttpServletRequest request) {
        revokeSession(Trail.CAPACITA, database(capacitaJdbc, "Capacita"), "student_users", email, request);
    }

    private void revokeSession(Trail trail, JdbcTemplate jdbc, String table, String email,
            jakarta.servlet.http.HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (email == null || authorization == null || !authorization.startsWith("Bearer ")) unauthorized();
        var decoded = tokens.verify(authorization.substring(7).trim(), trail);
        if (decoded == null || !email.equalsIgnoreCase(decoded.getSubject())) unauthorized();
        int version = decoded.getClaim("ver").asInt();
        int revoked = jdbc.update("UPDATE " + table + " SET token_version=token_version+1 WHERE id=? AND LOWER(email)=LOWER(?) AND token_version=?",
                decoded.getClaim("uid").asLong(), email, version);
        if (revoked != 1) unauthorized();
    }

    private Map<String, String> requestReset(JdbcTemplate jdbc, String table, String trailName, String email,
            jakarta.servlet.http.HttpServletRequest http) {
        limit("reset:" + trailName + ":" + http.getRemoteAddr(), 5, 60);
        if (!mailEnabled || mailSenders.getIfAvailable() == null)
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "A recuperação de senha desta trilha depende da configuração de e-mail.");
        List<Long> ids = jdbc.query("SELECT id FROM " + table + " WHERE email=? AND account_status='ACTIVE'", (rs, row) -> rs.getLong(1), email);
        if (!ids.isEmpty()) {
            String token = UUID.randomUUID().toString();
            jdbc.update("UPDATE " + table + " SET password_reset_token_hash=?,password_reset_expires_at=? WHERE id=?",
                    hash(token), java.sql.Timestamp.from(Instant.now().plus(30, ChronoUnit.MINUTES)), ids.get(0));
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(email);
            message.setSubject("Redefina sua senha do Trampolim " + trailName);
            message.setText("Use este link em até 30 minutos para redefinir sua senha: " + frontendUrl + "/#" + trailName.toLowerCase(Locale.ROOT) + "/reset?token=" + token);
            mailSenders.getIfAvailable().send(message);
        }
        return Map.of("message", "Se a conta estiver ativa, as instruções serão enviadas ao endereço informado.");
    }

    private Map<String, String> resetPassword(JdbcTemplate jdbc, String table, PasswordReset request) {
        String token = request == null ? "" : clean(request.token());
        String password = request == null || request.password() == null ? "" : request.password();
        PasswordPolicy.validate(password);
        int updated = jdbc.update("UPDATE " + table + " SET password_hash=?,token_version=token_version+1,failed_login_attempts=0,login_locked_until=NULL,password_reset_token_hash=NULL,password_reset_expires_at=NULL WHERE password_reset_token_hash=? AND password_reset_expires_at>CURRENT_TIMESTAMP AND account_status='ACTIVE'",
                passwordEncoder.encode(password), hash(token));
        if (updated == 0) bad("O link expirou ou já foi utilizado. Solicite outro.");
        return Map.of("message", "Senha alterada. As sessões anteriores foram encerradas.");
    }

    private void verifyEmailLater(JdbcTemplate jdbc, String table, long id, String email, String trailName) {
        String token = UUID.randomUUID().toString();
        jdbc.update("UPDATE " + table + " SET email_verification_token_hash=?,email_verification_expires_at=? WHERE id=?",
                hash(token), java.sql.Timestamp.from(Instant.now().plus(24, ChronoUnit.HOURS)), id);
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("Confirme seu e-mail no Trampolim " + trailName);
        message.setText("Confirme seu e-mail em até 24 horas: " + frontendUrl + "/#" + trailName.toLowerCase(Locale.ROOT) + "/verify?token=" + token);
        JavaMailSender sender = mailSenders.getIfAvailable();
        if (sender == null) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "O envio de confirmação não está configurado.");
        sender.send(message);
    }

    private Map<String, String> verifyEmail(JdbcTemplate jdbc, String table, String rawToken) {
        int updated = jdbc.update("UPDATE " + table + " SET email_verified=TRUE,email_verification_token_hash=NULL,email_verification_expires_at=NULL WHERE email_verification_token_hash=? AND email_verification_expires_at>CURRENT_TIMESTAMP AND account_status='ACTIVE'",
                hash(clean(rawToken)));
        if (updated == 0) bad("O link expirou ou já foi utilizado.");
        return Map.of("message", "E-mail confirmado. Agora você pode entrar nesta trilha.");
    }

    private Map<String, String> resendVerification(JdbcTemplate jdbc, String table, String trailName, String email) {
        requireMailConfigured();
        List<Long> ids = jdbc.query("SELECT id FROM " + table + " WHERE email=? AND email_verified=FALSE AND account_status='ACTIVE'", (rs, row) -> rs.getLong(1), email);
        if (!ids.isEmpty()) verifyEmailLater(jdbc, table, ids.get(0), email, trailName);
        return Map.of("message", "Se a conta precisar de confirmação, enviaremos um novo link para o endereço informado.");
    }

    private AuthResponse response(Trail trail, long id, String email, String name, boolean verified, int version, String status) {
        return new AuthResponse(verified ? tokens.issue(trail, email, id, version) : "", new UserSummary(name, email), verified,
                trail == Trail.EMPRESAS ? "COMPANY" : "STUDENT", status);
    }
    private JdbcTemplate database(ObjectProvider<JdbcTemplate> provider, String name) {
        JdbcTemplate jdbc = provider.getIfAvailable();
        if (jdbc == null) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "O banco independente de " + name + " ainda não está configurado.");
        return jdbc;
    }
    private void validateEmailAndPassword(String email, String password) {
        if (!email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) bad("Informe um e-mail válido.");
        PasswordPolicy.validate(password);
    }
    private void requireMailConfigured() {
        if (mailEnabled && mailSenders.getIfAvailable() == null)
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "O envio de confirmação não está configurado.");
    }
    private String email(String value) { return value == null ? "" : value.trim().toLowerCase(Locale.ROOT); }
    private String clean(String value) { return value == null ? "" : value.trim(); }
    private String limit(String value, int size) { return value.length() <= size ? value : value.substring(0, size); }
    private void unauthorized() { throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "E-mail ou senha incorretos."); }
    private void bad(String message) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message); }
    private void limit(String key,int maximum,int minutes) {
        if (!rateLimits.allow(key,maximum,java.time.Duration.ofMinutes(minutes))) throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,"Muitas tentativas. Tente novamente mais tarde.");
    }
    private String hash(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (java.security.NoSuchAlgorithmException exception) { throw new IllegalStateException(exception); }
    }
}
