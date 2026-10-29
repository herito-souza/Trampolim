package com.trampolim.api.company;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

class PublicCompanyControllerVisibilityTest {
    @Test
    @SuppressWarnings("unchecked")
    void internshipFeedReturnsNoFakeRowsAndRequiresEveryPublicationCondition() {
        JdbcTemplate empresas = mock(JdbcTemplate.class);
        doReturn(List.of()).when(empresas).query(anyString(), any(RowMapper.class), any(Object[].class));
        PublicCompanyController controller = new PublicCompanyController(empresas);

        List<PublicCompanyController.PublicInternship> opportunities = controller.internships(null, null, null, null);

        assertTrue(opportunities.isEmpty());
        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(empresas).query(sql.capture(), any(RowMapper.class), any(Object[].class));
        String query = sql.getValue();
        assertTrue(query.contains("c.verification_status='APPROVED'"));
        assertTrue(query.contains("l.kind='job'"));
        assertTrue(query.contains("l.is_internship=TRUE"));
        assertTrue(query.contains("l.payment_status='PAID'"));
        assertTrue(query.contains("l.publication_status='PUBLISHED'"));
        assertTrue(query.contains("l.expires_at IS NULL OR l.expires_at>=CURRENT_DATE"));
        assertTrue(query.contains("l.application_deadline IS NULL OR l.application_deadline>=CURRENT_DATE"));
        verifyNoMoreInteractions(empresas);
    }
    @Test
    @SuppressWarnings("unchecked")
    void companyProfileAppliesApplicationDeadlineAlongsidePublicationAndPayment() {
        JdbcTemplate database = mock(JdbcTemplate.class);
        Object[] company = {1L, "company", "Company", "Technology", "City", "SP", null, null};
        doReturn(java.util.Collections.singletonList(company), List.of()).when(database)
                .query(anyString(), any(RowMapper.class), any(Object[].class));
        var profile = new PublicCompanyController(database).profile("company");
        assertTrue(profile.listings().isEmpty());
        var sql = ArgumentCaptor.forClass(String.class);
        org.mockito.Mockito.verify(database, org.mockito.Mockito.times(2)).query(sql.capture(), any(RowMapper.class), any(Object[].class));
        String listingQuery = sql.getAllValues().get(1);
        assertTrue(listingQuery.contains("application_deadline IS NULL OR application_deadline>=CURRENT_DATE"));
        assertTrue(listingQuery.contains("expires_at IS NULL OR expires_at>=CURRENT_DATE"));
        assertTrue(listingQuery.contains("publication_status='PUBLISHED'"));
        assertTrue(listingQuery.contains("is_internship=FALSE OR payment_status='PAID'"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void unavailableListingCannotRecordPublicInteraction() {
        JdbcTemplate database = mock(JdbcTemplate.class);
        doReturn(List.of()).when(database).query(anyString(), any(RowMapper.class), any(Object[].class));
        var controller = new PublicCompanyController(database);
        org.junit.jupiter.api.Assertions.assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> controller.view("company", 10L));
        var sql = ArgumentCaptor.forClass(String.class);
        verify(database).query(sql.capture(), any(RowMapper.class), any(Object[].class));
        assertTrue(sql.getValue().contains("l.application_deadline IS NULL OR l.application_deadline>=CURRENT_DATE"));
        verifyNoMoreInteractions(database);
    }
}