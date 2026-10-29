package com.trampolim.api.education;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.dao.DataAccessResourceFailureException;
class PublicLearningSummaryTest {
    @Test void unavailableIsDistinctFromZeroPublishedContents() {
        var jdbc=mock(JdbcTemplate.class);
        String query="SELECT COUNT(*) FROM education_contents WHERE status='PUBLISHED'";
        when(jdbc.queryForObject(query,Long.class)).thenReturn(0L).thenThrow(new DataAccessResourceFailureException("test unavailable"));
        var summary=new PublicLearningSummaryController(jdbc);
        assertEquals(0L,summary.summary().publishedContents());
        assertNull(summary.summary().publishedContents());
    }
}
