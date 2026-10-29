package com.trampolim.api.organization;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/account/roles")
@RequiredArgsConstructor
public class UserRoleController {
    private final JdbcTemplate jdbc;
    public record UserRole(String role, String source, String createdAt) {}

    @GetMapping
    public List<UserRole> myRoles(@AuthenticationPrincipal String email) {
        return jdbc.query("SELECT r.role,r.assignment_source,r.created_at FROM app_user_roles r JOIN app_users u ON u.id=r.user_id WHERE LOWER(u.email)=LOWER(?) ORDER BY r.role", (rs, row) -> new UserRole(rs.getString(1), rs.getString(2), rs.getTimestamp(3).toInstant().toString()), email);
    }

    @PutMapping("/student")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void identifyAsStudent(@AuthenticationPrincipal String email) {
        Long userId = userId(email);
        jdbc.update("INSERT INTO app_user_roles(user_id,role,assignment_source) VALUES (?,'STUDENT','SELF_DECLARED') ON CONFLICT(user_id,role) DO NOTHING", userId);
        jdbc.update("INSERT INTO organization_audit_events(actor_user_id,event_type,entity_type) VALUES (?,'USER_ROLE_SELF_DECLARED','ROLE')", userId);
    }

    @DeleteMapping("/student")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeStudentRole(@AuthenticationPrincipal String email) {
        Long userId = userId(email);
        jdbc.update("DELETE FROM app_user_roles WHERE user_id=? AND role='STUDENT' AND assignment_source='SELF_DECLARED'", userId);
        jdbc.update("INSERT INTO organization_audit_events(actor_user_id,event_type,entity_type) VALUES (?,'USER_ROLE_SELF_REMOVED','ROLE')", userId);
    }

    private Long userId(String email) {
        return jdbc.query("SELECT id FROM app_users WHERE LOWER(email)=LOWER(?)", (rs, row) -> rs.getLong(1), email).stream().findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sessão inválida."));
    }
}
