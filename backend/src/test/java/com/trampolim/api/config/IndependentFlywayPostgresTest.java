package com.trampolim.api.config;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Properties;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class IndependentFlywayPostgresTest {
    @Test void allThreeMigrationHistoriesInitializeEmptySchemasIndependently() {
        verify("OPORTUNIDADES",env("DATABASE_URL","jdbc:postgresql://localhost:5432/trampolim_db"),env("DATABASE_USERNAME","root"),env("DATABASE_PASSWORD","root"),"db/migration","app_users","company_users");
        verify("EMPRESAS","jdbc:postgresql://localhost:5433/trampolim_empresas_db","trampolim_empresas","dev_empresas_only","db/migration-empresas","company_users","student_users");
        verify("CAPACITA","jdbc:postgresql://localhost:5434/trampolim_capacita_db","trampolim_capacita","dev_capacita_only","db/migration-capacita","student_users","company_users");
    }
    private void verify(String prefix,String fallbackUrl,String fallbackUser,String fallbackPassword,String location,String requiredTable,String forbiddenTable) {
        var source=new DriverManagerDataSource(env(prefix+"_DB_URL",fallbackUrl),env(prefix+"_DB_USERNAME",fallbackUser),env(prefix+"_DB_PASSWORD",fallbackPassword));
        var admin=new JdbcTemplate(source);
        String schema="flyway_test_"+UUID.randomUUID().toString().replace("-","");
        admin.execute("CREATE SCHEMA "+schema);
        try {
            var scoped=new DriverManagerDataSource(source.getUrl(),source.getUsername(),source.getPassword());
            var properties=new Properties(); properties.setProperty("currentSchema",schema); scoped.setConnectionProperties(properties);
            var flyway=Flyway.configure().dataSource(scoped).schemas(schema).defaultSchema(schema).locations("classpath:"+location).load();
            assertTrue(flyway.migrate().migrationsExecuted>0);
            flyway.validate();
            assertEquals(0,flyway.migrate().migrationsExecuted);
            var jdbc=new JdbcTemplate(scoped);
            assertEquals(1L,jdbc.queryForObject("SELECT count(*) FROM information_schema.tables WHERE table_schema=? AND table_name=?",Long.class,schema,requiredTable));
            assertEquals(0L,jdbc.queryForObject("SELECT count(*) FROM information_schema.tables WHERE table_schema=? AND table_name=?",Long.class,schema,forbiddenTable));
        } finally { admin.execute("DROP SCHEMA "+schema+" CASCADE"); }
    }
    private String env(String name,String fallback) { String value=System.getenv(name); return value==null||value.isBlank()?fallback:value; }
}
