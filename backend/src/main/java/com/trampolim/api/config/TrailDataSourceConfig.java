package com.trampolim.api.config;

import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.orm.jpa.JpaTransactionManager;
import jakarta.persistence.EntityManagerFactory;
import java.sql.SQLException;
import org.flywaydb.core.api.FlywayException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Explicit JDBC boundaries for the three data stores. The legacy Oportunidades database remains
 * primary until all historical data is migrated. Each additional store can be disabled explicitly;
 * connection failures during its migrations do not take down unrelated trails.
 */
@Configuration
public class TrailDataSourceConfig {
    private static final Logger log = LoggerFactory.getLogger(TrailDataSourceConfig.class);
    @Bean(name = "oportunidadesDataSourceProperties")
    @Primary
    @ConfigurationProperties("spring.datasource")
    public DataSourceProperties oportunidadesDataSourceProperties() {
        return new DataSourceProperties();
    }

    @org.springframework.context.annotation.DependsOn("trailDatabaseIsolationGuard")
    @Bean(name = "dataSource", destroyMethod = "close")
    @ConfigurationProperties("spring.datasource.hikari")
    @Primary
    public HikariDataSource oportunidadesDataSource(
            @Qualifier("oportunidadesDataSourceProperties") DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder().type(HikariDataSource.class).build();
    }

    @Bean(name = "jdbcTemplate")
    @Primary
    public JdbcTemplate oportunidadesJdbcTemplate(@Qualifier("dataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    /**
     * Keeps the legacy Oportunidades JPA repositories and JDBC operations on the same
     * primary transaction boundary. Secondary trails use their named JDBC managers.
     */
    @Bean(name = {"transactionManager", "oportunidadesTransactionManager"})
    @Primary
    public PlatformTransactionManager oportunidadesTransactionManager(
            @Qualifier("entityManagerFactory") EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }

    @Bean(name = "empresasDataSourceProperties")
    @ConfigurationProperties("trampolim.datasource.empresas")
    @ConditionalOnProperty(name = "trampolim.datasource.empresas.enabled", havingValue = "true")
    public DataSourceProperties empresasDataSourceProperties() {
        return new DataSourceProperties();
    }

    @org.springframework.context.annotation.DependsOn("trailDatabaseIsolationGuard")
    @Bean(name = "empresasDataSource", destroyMethod = "close")
    @ConfigurationProperties("trampolim.datasource.empresas.hikari")
    @ConditionalOnProperty(name = "trampolim.datasource.empresas.enabled", havingValue = "true")
    public HikariDataSource empresasDataSource(
            @Qualifier("empresasDataSourceProperties") DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder().type(HikariDataSource.class).build();
    }

    @Bean(name = "empresasJdbcTemplate")
    @ConditionalOnProperty(name = "trampolim.datasource.empresas.enabled", havingValue = "true")
    public JdbcTemplate empresasJdbcTemplate(@Qualifier("empresasDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean(name = "empresasTransactionManager")
    @ConditionalOnProperty(name = "trampolim.datasource.empresas.enabled", havingValue = "true")
    public PlatformTransactionManager empresasTransactionManager(@Qualifier("empresasDataSource") DataSource dataSource) {
        return new JdbcTransactionManager(dataSource);
    }

    @Bean(name = "empresasFlywayMigration")
    @ConditionalOnProperty(name = "trampolim.datasource.empresas.enabled", havingValue = "true")
    public InitializingBean empresasFlywayMigration(@Qualifier("empresasDataSource") DataSource dataSource) {
        return () -> migrateSecondary(dataSource, "db/migration-empresas", "Empresas");
    }

    @Bean(name = "capacitaDataSourceProperties")
    @ConfigurationProperties("trampolim.datasource.capacita")
    @ConditionalOnProperty(name = "trampolim.datasource.capacita.enabled", havingValue = "true")
    public DataSourceProperties capacitaDataSourceProperties() {
        return new DataSourceProperties();
    }

    @org.springframework.context.annotation.DependsOn("trailDatabaseIsolationGuard")
    @Bean(name = "capacitaDataSource", destroyMethod = "close")
    @ConfigurationProperties("trampolim.datasource.capacita.hikari")
    @ConditionalOnProperty(name = "trampolim.datasource.capacita.enabled", havingValue = "true")
    public HikariDataSource capacitaDataSource(
            @Qualifier("capacitaDataSourceProperties") DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder().type(HikariDataSource.class).build();
    }

    @Bean(name = "capacitaJdbcTemplate")
    @ConditionalOnProperty(name = "trampolim.datasource.capacita.enabled", havingValue = "true")
    public JdbcTemplate capacitaJdbcTemplate(@Qualifier("capacitaDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean(name = "capacitaTransactionManager")
    @ConditionalOnProperty(name = "trampolim.datasource.capacita.enabled", havingValue = "true")
    public PlatformTransactionManager capacitaTransactionManager(@Qualifier("capacitaDataSource") DataSource dataSource) {
        return new JdbcTransactionManager(dataSource);
    }

    @Bean(name = "capacitaFlywayMigration")
    @ConditionalOnProperty(name = "trampolim.datasource.capacita.enabled", havingValue = "true")
    public InitializingBean capacitaFlywayMigration(@Qualifier("capacitaDataSource") DataSource dataSource) {
        return () -> migrateSecondary(dataSource, "db/migration-capacita", "Capacita");
    }

    private void migrateSecondary(DataSource dataSource, String location, String trail) {
        try {
            Flyway.configure().dataSource(dataSource).locations("classpath:" + location).load().migrate();
        } catch (FlywayException exception) {
            if (!isConnectionFailure(exception)) throw exception;
            log.error("Banco de {} indisponível durante a inicialização; a aplicação seguirá ativa e as rotas desta trilha responderão com indisponibilidade.", trail);
        }
    }

    private boolean isConnectionFailure(Throwable failure) {
        for (Throwable current = failure; current != null; current = current.getCause()) {
            if (current instanceof SQLException sqlException && sqlException.getSQLState() != null
                    && sqlException.getSQLState().startsWith("08")) return true;
        }
        return false;
    }
}
