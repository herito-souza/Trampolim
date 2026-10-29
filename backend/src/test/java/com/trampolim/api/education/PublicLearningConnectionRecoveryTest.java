package com.trampolim.api.education;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class PublicLearningConnectionRecoveryTest {
    @Test void connectionFailureIsUnavailableAndServiceRecoversWhenDatabaseReturns() throws Exception {
        int closedPort;
        try(var reservation=new java.net.ServerSocket(0,1,java.net.InetAddress.getLoopbackAddress())) { closedPort=reservation.getLocalPort(); }
        var source=new DriverManagerDataSource("jdbc:postgresql://localhost:"+closedPort+"/unavailable?connectTimeout=1&socketTimeout=1","unavailable","unused-test-value");
        var summary=new PublicLearningSummaryController(new JdbcTemplate(source));
        assertNull(summary.summary().publishedContents());
        source.setUrl(env("CAPACITA_DB_URL","jdbc:postgresql://localhost:5434/trampolim_capacita_db"));
        source.setUsername(env("CAPACITA_DB_USERNAME","trampolim_capacita"));
        source.setPassword(env("CAPACITA_DB_PASSWORD","dev_capacita_only"));
        Long recovered=summary.summary().publishedContents();
        assertNotNull(recovered);
        assertTrue(recovered>=0);
    }
    private String env(String name,String fallback) {String value=System.getenv(name);return value==null||value.isBlank()?fallback:value;}
}
