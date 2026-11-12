package com.trampolim.api.fairs;
import org.springframework.context.annotation.*;import org.springframework.beans.factory.annotation.Qualifier;import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;import org.springframework.jdbc.core.JdbcTemplate;import org.springframework.transaction.PlatformTransactionManager;import com.trampolim.api.radar.RadarBridge;
@Configuration public class FairRegistrationConfig {
 @Bean("candidateFairRegistrations")FairRegistrationStore candidates(@Qualifier("jdbcTemplate")JdbcTemplate db,@Qualifier("oportunidadesTransactionManager")PlatformTransactionManager manager,RadarBridge bridge){return new FairRegistrationStore(db,manager,bridge,false);}
 @Bean("studentFairRegistrations")@ConditionalOnProperty(name="trampolim.datasource.capacita.enabled",havingValue="true")FairRegistrationStore students(@Qualifier("capacitaJdbcTemplate")JdbcTemplate db,@Qualifier("capacitaTransactionManager")PlatformTransactionManager manager,RadarBridge bridge){return new FairRegistrationStore(db,manager,bridge,true);}
}
