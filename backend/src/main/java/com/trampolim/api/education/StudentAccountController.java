package com.trampolim.api.education;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/capacita/account")
@ConditionalOnProperty(name = "trampolim.datasource.capacita.enabled", havingValue = "true")
public class StudentAccountController {
    private final java.util.concurrent.ConcurrentHashMap<String, Attempts> attempts = new java.util.concurrent.ConcurrentHashMap<>();
    private record Attempts(java.time.Instant until, int count) {}
    private final JdbcTemplate students;
    private final PasswordEncoder passwords;
    public StudentAccountController(@Qualifier("capacitaJdbcTemplate") JdbcTemplate students, PasswordEncoder passwords) {
        this.students = students;
        this.passwords = passwords;
    }
    public record DeleteAccount(String password) {}

    public record AgeDeclaration(String ageBand) {}
    @GetMapping("/age-declaration")
    public AgeDeclaration ageDeclaration(@AuthenticationPrincipal String email) {
        if(email==null || email.isBlank()) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        var rows=students.query("SELECT age_band FROM student_users WHERE LOWER(email)=LOWER(?) AND account_status='ACTIVE'",
                (rs,row)->new AgeDeclaration(rs.getString(1)),email);
        return rows.stream().findFirst().orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
    @PutMapping("/age-declaration")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void declareAge(@AuthenticationPrincipal String email,@RequestBody AgeDeclaration declaration) {
        if(email==null || email.isBlank()) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        if(declaration==null || declaration.ageBand()==null || !java.util.Set.of("AGE_14_17","AGE_18_PLUS").contains(declaration.ageBand()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Selecione uma faixa etária válida. O Capacita atende pessoas a partir de 14 anos.");
        int changed=students.update("UPDATE student_users SET age_band=?,age_declared_at=CURRENT_TIMESTAMP WHERE LOWER(email)=LOWER(?) AND account_status='ACTIVE'",declaration.ageBand(),email);
        if(changed==0) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    }
    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional(transactionManager = "capacitaTransactionManager")
    public void deleteAccount(@AuthenticationPrincipal String email, @RequestBody DeleteAccount request) {
        if (email == null || email.isBlank()) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        if (request == null || request.password() == null || request.password().isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Confirme sua senha atual para excluir a conta.");
        limitAttempts(email);
        var accounts = students.queryForList("SELECT id,password_hash FROM student_users WHERE LOWER(email)=LOWER(?) AND account_status='ACTIVE' FOR UPDATE", email);
        if (accounts.size() != 1 || !passwords.matches(request.password(), (String) accounts.get(0).get("password_hash")))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Não foi possível confirmar as credenciais.");
        // Progress references the student with ON DELETE CASCADE. No other trail is accessed.
        students.update("DELETE FROM student_users WHERE id=?", accounts.get(0).get("id"));
    }
    private void limitAttempts(String email) {
        var now = java.time.Instant.now();
        String key = email.toLowerCase(java.util.Locale.ROOT);
        var bucket = attempts.compute(key, (ignored, previous) -> previous == null || !previous.until().isAfter(now)
                ? new Attempts(now.plusSeconds(900), 1) : new Attempts(previous.until(), previous.count() + 1));
        if (attempts.size() > 10_000) attempts.entrySet().removeIf(entry -> !entry.getValue().until().isAfter(now));
        if (bucket.count() > 5) throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                "Muitas tentativas de confirmação. Aguarde 15 minutos e tente novamente.");
    }
}