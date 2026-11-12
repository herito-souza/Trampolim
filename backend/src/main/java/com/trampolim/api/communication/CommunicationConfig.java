package com.trampolim.api.communication;
import org.springframework.context.annotation.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
@Configuration public class CommunicationConfig {
 @Bean("opportunitiesCommunications") CommunicationStore opportunities(@Qualifier("jdbcTemplate")JdbcTemplate db,@Qualifier("oportunidadesTransactionManager")PlatformTransactionManager tx){return new CommunicationStore(db,tx,"OPORTUNIDADES");}
 @Bean("companyCommunications") @ConditionalOnProperty(name="trampolim.datasource.empresas.enabled",havingValue="true") CommunicationStore company(@Qualifier("empresasJdbcTemplate")JdbcTemplate db,@Qualifier("empresasTransactionManager")PlatformTransactionManager tx){return new CommunicationStore(db,tx,"EMPRESAS");}
 @Bean("learningCommunications") @ConditionalOnProperty(name="trampolim.datasource.capacita.enabled",havingValue="true") CommunicationStore learning(@Qualifier("capacitaJdbcTemplate")JdbcTemplate db,@Qualifier("capacitaTransactionManager")PlatformTransactionManager tx){return new CommunicationStore(db,tx,"CAPACITA");}
}
