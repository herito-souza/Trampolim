package com.trampolim.api.config;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
class CorsConfigSecurityTest {
 static class Registry extends CorsRegistry {Map<String,CorsConfiguration> values(){return getCorsConfigurations();}}
 @Test void exactOriginsAreTrimmedAndCookiesAreNotAuthorized(){var config=new CorsConfig();ReflectionTestUtils.setField(config,"allowedOrigins"," https://one.example.test , https://two.example.test ");ReflectionTestUtils.setField(config,"allowedOriginPatterns","");var registry=new Registry();config.addCorsMappings(registry);var cors=registry.values().get("/**");assertEquals(List.of("https://one.example.test","https://two.example.test"),cors.getAllowedOrigins());assertEquals(Boolean.FALSE,cors.getAllowCredentials());assertNull(cors.checkOrigin("https://attacker.example.test"));assertEquals("https://one.example.test",cors.checkOrigin("https://one.example.test"));assertTrue(cors.checkHeaders(List.of("Authorization","Content-Type")).contains("Authorization"));}
}
