package com.trampolim.api.company;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.support.JdbcTransactionManager;

class LegacyOrganizationPostgresTest {
    @Test
    void migratesAcrossSeparateDatabasesAndRepeatsSafely() throws Exception {
        String sourceUrl = env("OPORTUNIDADES_DB_URL", env("DATABASE_URL", "jdbc:postgresql://localhost:5432/trampolim_db"));
        String targetUrl = env("EMPRESAS_DB_URL", "jdbc:postgresql://localhost:5433/trampolim_empresas_db");
        String sourceUser = env("OPORTUNIDADES_DB_USERNAME", env("DATABASE_USERNAME", "root"));
        String sourcePassword = env("OPORTUNIDADES_DB_PASSWORD", env("DATABASE_PASSWORD", "root"));
        String targetUser = env("EMPRESAS_DB_USERNAME", "trampolim_empresas");
        String targetPassword = env("EMPRESAS_DB_PASSWORD", "dev_empresas_only");
        String schema = "migration_test_" + UUID.randomUUID().toString().replace("-", "");
        var sourceAdmin = source(sourceUrl, sourceUser, sourcePassword);
        var targetAdmin = source(targetUrl, targetUser, targetPassword);
        sourceAdmin.execute("CREATE SCHEMA " + schema);
        try {
            targetAdmin.execute("CREATE SCHEMA " + schema);
            try {
                var source = scopedSource(sourceUrl, sourceUser, sourcePassword, schema);
                var target = scopedSource(targetUrl, targetUser, targetPassword, schema);
                source.execute("CREATE TABLE organizations(id BIGINT PRIMARY KEY,name TEXT,organization_type TEXT,status TEXT,legacy_company_id BIGINT,created_at TIMESTAMPTZ DEFAULT now())");
                source.execute("CREATE TABLE app_users(id BIGINT PRIMARY KEY,email TEXT,full_name TEXT)");
                source.execute("CREATE TABLE organization_members(organization_id BIGINT,user_id BIGINT,role TEXT,created_at TIMESTAMPTZ DEFAULT now())");
                source.execute("CREATE TABLE organization_relationships(parent_organization_id BIGINT,child_organization_id BIGINT,relationship_type TEXT,status TEXT,created_at TIMESTAMPTZ DEFAULT now())");
                target.execute("CREATE TABLE companies(id BIGINT PRIMARY KEY,legacy_company_id BIGINT UNIQUE)");
                target.execute(new ClassPathResource("db/migration-empresas/V8__independent_organizations_and_associations.sql").getContentAsString(StandardCharsets.UTF_8));
                source.update("INSERT INTO organizations(id,name,organization_type,status,legacy_company_id) VALUES (1,'Association','ASSOCIATION','ACTIVE',null),(2,'Company','COMPANY','ACTIVE',22),(3,'Pending company','COMPANY','ACTIVE',33)");
                source.update("INSERT INTO app_users VALUES (5,'member@example.test','Member')");
                source.update("INSERT INTO organization_members(organization_id,user_id,role) VALUES (1,5,'ASSOCIATION_ADMIN')");
                source.update("INSERT INTO organization_relationships(parent_organization_id,child_organization_id,relationship_type,status) VALUES (1,2,'ASSOCIATION_MEMBER','ACTIVE'),(1,3,'ASSOCIATION_MEMBER','PENDING')");
                target.update("INSERT INTO companies VALUES (222,22)");
                var service = new LegacyOrganizationMigrationService(source,target,new JdbcTransactionManager(target.getDataSource()));
                assertEquals(1,service.preview().relationshipsWithoutMigratedCompany());
                var first = service.migrate("operator@example.test");
                assertTrue(first.organizationErrors().isEmpty());
                assertEquals(1,first.importedOrganizations());
                assertEquals(1,first.importedMemberships());
                assertEquals(1,first.importedRelationships());
                var second = service.migrate("operator@example.test");
                assertEquals(0,second.importedOrganizations());
                assertEquals(0,second.importedMemberships());
                assertEquals(0,second.importedRelationships());
                assertEquals(222L,target.queryForObject("SELECT company_id FROM enterprise_organization_companies",Long.class));
                assertEquals(2L,source.queryForObject("SELECT count(*) FROM organization_relationships",Long.class));
            } finally { targetAdmin.execute("DROP SCHEMA " + schema + " CASCADE"); }
        } finally { sourceAdmin.execute("DROP SCHEMA " + schema + " CASCADE"); }
    }
    private String env(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }
    private JdbcTemplate scopedSource(String url, String user, String password, String schema) {
        var dataSource = new DriverManagerDataSource(url, user, password);
        var properties = new java.util.Properties();
        properties.setProperty("currentSchema", schema);
        dataSource.setConnectionProperties(properties);
        return new JdbcTemplate(dataSource);
    }
    private JdbcTemplate source(String url,String user,String password) {
        return new JdbcTemplate(new DriverManagerDataSource(url,user,password));
    }
}
