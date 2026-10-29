package com.trampolim.api.company;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
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
@RequestMapping("/api/empresas/account")
@ConditionalOnProperty(name="trampolim.datasource.empresas.enabled",havingValue="true")
public class CompanyAccountDeletionController {
    private final JdbcTemplate companies;
    private final PasswordEncoder passwords;
    private final ConcurrentHashMap<String,Attempts> attempts=new ConcurrentHashMap<>();
    private record Attempts(Instant until,int count) {}
    public record Confirmation(String password) {}
    public CompanyAccountDeletionController(@Qualifier("empresasJdbcTemplate") JdbcTemplate companies,PasswordEncoder passwords) {
        this.companies=companies; this.passwords=passwords;
    }
    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional(transactionManager="empresasTransactionManager")
    public void delete(@AuthenticationPrincipal String email,@RequestBody Confirmation confirmation) {
        if(email==null||email.isBlank()) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        if(confirmation==null||confirmation.password()==null||confirmation.password().isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Confirme sua senha atual.");
        Instant now=Instant.now();
        var attempt=attempts.compute(email,(key,old)->old==null||old.until().isBefore(now)
            ?new Attempts(now.plusSeconds(900),1):new Attempts(old.until(),old.count()+1));
        attempts.entrySet().removeIf(entry->entry.getValue().until().isBefore(now));
        if(attempt.count()>5) throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,"Tente novamente mais tarde.");
        var rows=companies.queryForList("SELECT id,password_hash FROM company_users WHERE LOWER(email)=LOWER(?) AND account_status='ACTIVE' FOR UPDATE",email);
        if(rows.size()!=1||!passwords.matches(confirmation.password(),(String)rows.get(0).get("password_hash")))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Não foi possível confirmar a senha atual.");
        long id=((Number)rows.get(0).get("id")).longValue();
        if(companies.queryForObject("SELECT count(*) FROM companies WHERE owner_user_id=?",Long.class,id)>0
            ||companies.queryForObject("SELECT count(*) FROM enterprise_organization_members WHERE LOWER(email)=LOWER(?) AND role IN ('ASSOCIATION_ADMIN','EDUCATION_ADMIN','PARTNER_MANAGER')",Long.class,email)>0)
            throw new ResponseStatusException(HttpStatus.CONFLICT,"Sua conta é responsável por uma organização. Transfira a responsabilidade com a administração antes de excluir seu acesso.");
        companies.update("DELETE FROM enterprise_organization_invites WHERE LOWER(email)=LOWER(?)",email);
        companies.update("DELETE FROM enterprise_organization_members WHERE LOWER(email)=LOWER(?)",email);
        companies.update("DELETE FROM company_users WHERE id=?",id);
        attempts.remove(email);
    }
}
