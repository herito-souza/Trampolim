package com.trampolim.api.organization;

import com.trampolim.api.security.PlatformAccess;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/** Explicit platform administration boundary; corporate/student sessions are not accepted here. */
@RestController
@RequestMapping("/api/admin/trail-accounts")
public class TrailAccountAdminController {
    private final PlatformAccess access;
    private final ObjectProvider<JdbcTemplate> companies;
    private final ObjectProvider<JdbcTemplate> students;
    public TrailAccountAdminController(PlatformAccess access,
            @Qualifier("empresasJdbcTemplate") ObjectProvider<JdbcTemplate> companies,
            @Qualifier("capacitaJdbcTemplate") ObjectProvider<JdbcTemplate> students) {
        this.access=access; this.companies=companies; this.students=students;
    }
    public record Account(long id,String email,String name,String status,boolean emailVerified,String createdAt) {}
    @GetMapping("/{trail}")
    public List<Account> list(@AuthenticationPrincipal String actor,@PathVariable String trail,
            @RequestParam(defaultValue="0") int offset,@RequestParam(defaultValue="50") int limit) {
        if (!access.isSuperAdmin(actor)) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        JdbcTemplate database;
        String table;
        switch (trail) {
            case "empresas" -> { database=companies.getIfAvailable(); table="company_users"; }
            case "capacita" -> { database=students.getIfAvailable(); table="student_users"; }
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Trilha inválida.");
        }
        if (database==null) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Esta trilha está indisponível.");
        if (offset<0 || limit<1 || limit>100) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Paginação inválida.");
        return database.query("SELECT id,email,full_name,account_status,email_verified,created_at FROM "+table+" ORDER BY id LIMIT ? OFFSET ?",
                (rs,row)->new Account(rs.getLong(1),rs.getString(2),rs.getString(3),rs.getString(4),rs.getBoolean(5),rs.getTimestamp(6).toInstant().toString()),limit,offset);
    }
    public record StatusChange(String status,String reason) {}
    @PutMapping("/{trail}/{id}/status")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changeStatus(@AuthenticationPrincipal String actor,@PathVariable String trail,
            @PathVariable long id,@RequestBody StatusChange input) {
        if (!access.isSuperAdmin(actor)) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        if (input==null || !java.util.Set.of("ACTIVE","SUSPENDED").contains(input.status()==null?"":input.status())
                || input.reason()==null || input.reason().isBlank() || input.reason().length()>500)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Informe status e justificativa de até 500 caracteres.");
        JdbcTemplate database;
        String table;
        switch (trail) {
            case "empresas" -> { database=companies.getIfAvailable(); table="company_users"; }
            case "capacita" -> { database=students.getIfAvailable(); table="student_users"; }
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Trilha inválida.");
        }
        if (database==null) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Esta trilha está indisponível.");
        // A single PostgreSQL statement makes the status change, session revocation and audit atomic.
        int changed = database.update("WITH previous AS (SELECT id,account_status FROM "+table+" WHERE id=? FOR UPDATE), changed AS (UPDATE "+table+" u SET account_status=?,token_version=token_version+1 FROM previous p WHERE u.id=p.id AND p.account_status<>? RETURNING u.id,p.account_status) INSERT INTO account_admin_audit(target_user_id,actor_email,previous_status,new_status,reason) SELECT id,?,account_status,?,? FROM changed",
                id,input.status(),input.status(),actor,input.status(),input.reason().trim());
        if (changed==0) {
            Long count=database.queryForObject("SELECT COUNT(*) FROM "+table+" WHERE id=?",Long.class,id);
            if(count==null || count==0) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Conta não encontrada.");
        }
    }
    public record AuditEntry(long id,long userId,String actor,String previousStatus,String newStatus,String reason,String createdAt) {}
    @GetMapping("/{trail}/{id}/audit")
    public List<AuditEntry> audit(@AuthenticationPrincipal String actor,@PathVariable String trail,@PathVariable long id,
            @RequestParam(defaultValue="0") int offset,@RequestParam(defaultValue="50") int limit) {
        if (!access.isSuperAdmin(actor)) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        JdbcTemplate database = switch(trail) {
            case "empresas" -> companies.getIfAvailable();
            case "capacita" -> students.getIfAvailable();
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Trilha inválida.");
        };
        if(database==null) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Esta trilha está indisponível.");
        if(id<1 || offset<0 || limit<1 || limit>100) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Consulta inválida.");
        return database.query("SELECT id,target_user_id,actor_email,previous_status,new_status,reason,created_at FROM account_admin_audit WHERE target_user_id=? ORDER BY id DESC LIMIT ? OFFSET ?",
                (rs,row)->new AuditEntry(rs.getLong(1),rs.getLong(2),rs.getString(3),rs.getString(4),rs.getString(5),rs.getString(6),rs.getTimestamp(7).toInstant().toString()),id,limit,offset);
    }
}