package com.trampolim.api.events;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Configuration
public class DomainEventRelayConfiguration {
    @Bean @ConditionalOnProperty(name="trampolim.datasource.empresas.enabled",havingValue="true")
    DomainEventRelay empresasDomainEventRelay(@Qualifier("empresasJdbcTemplate") JdbcTemplate jdbc,
            @Qualifier("empresasTransactionManager") PlatformTransactionManager manager,
            @Value("${domain-events.retention-days:365}") int retentionDays) {
        return new DomainEventRelay("EMPRESAS", jdbc, new TransactionTemplate(manager),retentionDays);
    }

    @Bean @ConditionalOnProperty(name="trampolim.datasource.capacita.enabled",havingValue="true")
    DomainEventRelay capacitaDomainEventRelay(@Qualifier("capacitaJdbcTemplate") JdbcTemplate jdbc,
            @Qualifier("capacitaTransactionManager") PlatformTransactionManager manager,
            @Value("${domain-events.retention-days:365}") int retentionDays) {
        return new DomainEventRelay("CAPACITA", jdbc, new TransactionTemplate(manager),retentionDays);
    }

    @Bean @ConditionalOnProperty(name="spring.datasource.url")
    DomainEventRelay oportunidadesDomainEventRelay(@Qualifier("jdbcTemplate") JdbcTemplate jdbc,
            @Qualifier("oportunidadesTransactionManager") PlatformTransactionManager manager,
            @Value("${domain-events.retention-days:365}") int retentionDays) {
        return new DomainEventRelay("OPORTUNIDADES", jdbc, new TransactionTemplate(manager),retentionDays);
    }
}
