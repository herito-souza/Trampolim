package com.trampolim.api.privacy;
import org.springframework.context.annotation.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
@Configuration public class PrivacyConfig {
 @Bean("opportunitiesPrivacy")PrivacyStore opportunities(@Qualifier("jdbcTemplate")JdbcTemplate db,@Qualifier("oportunidadesTransactionManager")PlatformTransactionManager tx){return new PrivacyStore("oportunidades",db,tx);}
 @Bean("companiesPrivacy")@ConditionalOnProperty(name="trampolim.datasource.empresas.enabled",havingValue="true")PrivacyStore companies(@Qualifier("empresasJdbcTemplate")JdbcTemplate db,@Qualifier("empresasTransactionManager")PlatformTransactionManager tx){return new PrivacyStore("empresas",db,tx);}
 @Bean("learningPrivacy")@ConditionalOnProperty(name="trampolim.datasource.capacita.enabled",havingValue="true")PrivacyStore learning(@Qualifier("capacitaJdbcTemplate")JdbcTemplate db,@Qualifier("capacitaTransactionManager")PlatformTransactionManager tx){return new PrivacyStore("capacita",db,tx);}
}
