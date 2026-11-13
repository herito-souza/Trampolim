package com.trampolim.api.config.features;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Configuration
public class FeatureFlagsConfiguration {
    private static String activeEnvironment(Environment env){
        List<String> profiles=Arrays.stream(env.getActiveProfiles()).map(FeatureFlagsConfiguration::canonicalEnvironment).toList();
        for(String candidate:List.of("prod","staging","test","dev","local"))if(profiles.contains(candidate))return candidate;
        return profiles.isEmpty()?"local":profiles.getFirst();
    }
    private static String canonicalEnvironment(String profile){
        String value=profile.toLowerCase(Locale.ROOT);
        return switch(value){case "production" -> "prod";case "development" -> "dev";default -> value;};
    }
    @Bean @ConditionalOnProperty(name="spring.datasource.url")
    TrailFeatureFlags opportunitiesFlags(@Qualifier("jdbcTemplate")JdbcTemplate db,@Qualifier("oportunidadesTransactionManager")PlatformTransactionManager tx,ObjectMapper json,Environment env){return new TrailFeatureFlags("OPORTUNIDADES",activeEnvironment(env),db,new TransactionTemplate(tx),json);}
    @Bean @ConditionalOnProperty(name="trampolim.datasource.empresas.enabled",havingValue="true")
    TrailFeatureFlags companyFlags(@Qualifier("empresasJdbcTemplate")JdbcTemplate db,@Qualifier("empresasTransactionManager")PlatformTransactionManager tx,ObjectMapper json,Environment env){return new TrailFeatureFlags("EMPRESAS",activeEnvironment(env),db,new TransactionTemplate(tx),json);}
    @Bean @ConditionalOnProperty(name="trampolim.datasource.capacita.enabled",havingValue="true")
    TrailFeatureFlags learningFlags(@Qualifier("capacitaJdbcTemplate")JdbcTemplate db,@Qualifier("capacitaTransactionManager")PlatformTransactionManager tx,ObjectMapper json,Environment env){return new TrailFeatureFlags("CAPACITA",activeEnvironment(env),db,new TransactionTemplate(tx),json);}
}
