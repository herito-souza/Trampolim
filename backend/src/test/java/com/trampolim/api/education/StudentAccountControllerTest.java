package com.trampolim.api.education;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;
class StudentAccountControllerTest {
    @Test void repeatedWrongPasswordsAreLimitedBeforeFurtherDatabaseAccess() {
        var jdbc = mock(JdbcTemplate.class);
        var encoder = mock(PasswordEncoder.class);
        when(jdbc.queryForList(anyString(), any(Object[].class))).thenReturn(List.of(Map.of("id", 1L, "password_hash", "hash")));
        var controller = new StudentAccountController(jdbc, encoder);
        var request = new StudentAccountController.DeleteAccount("wrong-password");
        for (int index = 0; index < 5; index++) {
            assertEquals(401, assertThrows(ResponseStatusException.class, () -> controller.deleteAccount("student@example.test", request)).getStatusCode().value());
        }
        assertEquals(429, assertThrows(ResponseStatusException.class, () -> controller.deleteAccount("STUDENT@example.test", request)).getStatusCode().value());
        verify(jdbc, times(5)).queryForList(anyString(), any(Object[].class));
        verify(jdbc, never()).update(anyString(), any(Object[].class));
    }
}
