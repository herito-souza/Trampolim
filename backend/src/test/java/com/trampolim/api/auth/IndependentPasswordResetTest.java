package com.trampolim.api.auth;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;
import com.trampolim.api.security.TrailTokenService;

class IndependentPasswordResetTest {
    @Test
    void resetIsRestrictedToSelectedDatabaseAndRevokesOnlyThatTrailsSessions() {
        JdbcTemplate companies = mock(JdbcTemplate.class);
        JdbcTemplate students = mock(JdbcTemplate.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        when(encoder.encode("new-password-2026")).thenReturn("hashed-password");
        when(companies.update(anyString(), any(Object[].class))).thenReturn(1, 0);
        when(students.update(anyString(), any(Object[].class))).thenReturn(0);
        var controller = new IndependentTrailAuthController(provider(companies), provider(students), encoder,
                mock(TrailTokenService.class), provider(null));
        var reset = new IndependentTrailAuthController.PasswordReset("company-reset-token", "new-password-2026");
        assertNotNull(controller.resetCompany(reset));
        verifyNoInteractions(students);
        assertThrows(ResponseStatusException.class, () -> controller.resetStudent(reset));
        assertThrows(ResponseStatusException.class, () -> controller.resetCompany(reset));
        var sql = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(companies, times(2)).update(sql.capture(), any(Object[].class));
        for (String query : sql.getAllValues()) {
            assertTrue(query.startsWith("UPDATE company_users "));
            assertTrue(query.contains("token_version=token_version+1"));
            assertTrue(query.contains("password_reset_token_hash=NULL"));
            assertTrue(query.contains("password_reset_expires_at>CURRENT_TIMESTAMP"));
            assertTrue(query.contains("account_status='ACTIVE'"));
        }
    }
    @SuppressWarnings("unchecked")
    private <T> ObjectProvider<T> provider(T value) {
        ObjectProvider<T> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(value);
        return provider;
    }
}
