package com.trampolim.api.education;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.server.ResponseStatusException;
import com.trampolim.api.security.PlatformAccess;
class CapacitaExportTest {
    @Test void exportRequiresIdentityAndReturnsOnlyExplicitPrivateFields() {
        var jdbc = mock(JdbcTemplate.class);
        var platform = mock(PlatformAccess.class);
        var controller = new CapacitaController(jdbc, platform);
        assertThrows(ResponseStatusException.class, () -> controller.exportAccount(null));
        verifyNoInteractions(jdbc, platform);
        when(jdbc.queryForList(contains("FROM student_users"), eq("student@example.test")))
                .thenReturn(List.of(Map.of("id", 8L, "email", "student@example.test")));
        when(jdbc.queryForList(contains("FROM student_education_progress"), eq(8L)))
                .thenReturn(List.of(Map.of("content_id", 2L)));
        var result = controller.exportAccount("student@example.test");
        assertEquals("capacita", result.get("trail"));
        assertEquals(List.of(Map.of("content_id", 2L)), result.get("progress"));
        verify(jdbc).queryForList(startsWith("SELECT id,email,full_name,education_stage,institution_name,email_verified,terms_accepted_at,created_at,age_band,age_declared_at FROM student_users"), eq("student@example.test"));
        verify(jdbc).queryForList(startsWith("SELECT content_id,completed_at FROM student_education_progress WHERE student_user_id=?"), eq(8L));
        verifyNoInteractions(platform);
    }
}
