package com.trampolim.api.hub;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.web.server.ResponseStatusException;

class HubAccessTest {
    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final HubAccess access = new HubAccess(jdbc);

    @SuppressWarnings("unchecked")
    private void roles(String... values) {
        when(jdbc.query(anyString(), any(RowMapper.class), anyLong(), anyString()))
                .thenReturn(java.util.Arrays.stream(values).map(role -> new String[] {"EDUCATIONAL", role}).toList());
    }

    @Test void educationAdminCanManageAndIsReportedAsManager() {
        roles("EDUCATION_ADMIN");
        assertTrue(access.require("admin@school.example", 42, true));
        verify(jdbc).query(contains("verification_status='VERIFIED'"), any(RowMapper.class), eq(42L), eq("admin@school.example"));
    }

    @Test void contentEditorCanEditHubContentButCannotManageInstitutionProfile() {
        roles("CONTENT_EDITOR");
        assertFalse(access.require("editor@school.example", 42, true));
        assertThrows(ResponseStatusException.class, () -> access.requireAdmin("editor@school.example", 42));
    }

    @Test void partnerManagerIsReadOnlyAndUnverifiedInstitutionHasNoAccess() {
        roles("PARTNER_MANAGER");
        assertFalse(access.require("partner@school.example", 42, false));
        assertThrows(ResponseStatusException.class, () -> access.require("partner@school.example", 42, true));
        when(jdbc.query(anyString(), any(RowMapper.class), anyLong(), anyString())).thenReturn(List.of());
        assertThrows(ResponseStatusException.class, () -> access.require("admin@school.example", 42, false));
    }
}
