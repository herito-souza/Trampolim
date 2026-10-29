package com.trampolim.api.company;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

class LegacyOrganizationMigrationServiceTest {
    @Test
    @SuppressWarnings("unchecked")
    void readsLegacyRelationshipsOnlyFromSourceAndRetriesWithoutDuplicateLinks() {
        JdbcTemplate source = mock(JdbcTemplate.class);
        JdbcTemplate target = mock(JdbcTemplate.class);
        PlatformTransactionManager manager = mock(PlatformTransactionManager.class);
        when(manager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        Timestamp now = Timestamp.from(Instant.now());
        when(source.queryForList(contains("SELECT id,name,organization_type"))).thenReturn(List.of(
                Map.of("id", 7L, "name", "Association", "organization_type", "ASSOCIATION", "status", "ACTIVE", "created_at", now)));
        doReturn(List.of(70L)).when(target).query(contains("SELECT id FROM enterprise_organizations"), any(RowMapper.class), eq(7L));
        when(source.queryForList(contains("FROM organization_members"), eq(7L))).thenReturn(List.of());
        when(source.queryForList(contains("SELECT child.id AS child_id"), eq(7L))).thenReturn(List.of(
                Map.of("child_id", 8L, "legacy_company_id", 9L, "relationship_type", "MEMBER", "status", "ACTIVE", "created_at", now)));
        when(target.update(contains("INSERT INTO enterprise_organization_companies"), eq(70L), eq(8L), eq("MEMBER"), eq("ACTIVE"), eq(now), eq(9L)))
                .thenReturn(1, 0);
        var service = new LegacyOrganizationMigrationService(source, target, manager);
        assertEquals(1, service.migrate("operator@example.test").importedRelationships());
        assertEquals(0, service.migrate("operator@example.test").importedRelationships());
        verify(target, never()).queryForList(anyString(), any(Object[].class));
        verify(source, times(2)).queryForList(contains("SELECT child.id AS child_id"), eq(7L));
        verify(target, times(2)).update(contains("ON CONFLICT(organization_id,company_id,relationship_type) DO NOTHING"), eq(70L), eq(8L), eq("MEMBER"), eq("ACTIVE"), eq(now), eq(9L));
        verify(manager, times(2)).commit(any());
    }
}
