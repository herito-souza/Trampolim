package com.trampolim.api.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.beans.factory.annotation.Value;
import java.util.Arrays;

@Configuration
public class CorsConfig implements WebMvcConfigurer {
    @Value("${app.cors.allowed-origins:http://localhost:5173,http://127.0.0.1:5173}") private String allowedOrigins;
    @Value("${app.cors.allowed-origin-patterns:}") private String allowedOriginPatterns;
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        var cors = registry.addMapping("/**")
                .allowedOrigins(allowedOrigins.split(","))
                .allowedMethods("GET", "HEAD", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("Accept-Ranges","Content-Range","Content-Length","X-Request-ID")
                .allowCredentials(true);
        if (!allowedOriginPatterns.isBlank()) {
            cors.allowedOriginPatterns(Arrays.stream(allowedOriginPatterns.split(","))
                    .map(String::trim)
                    .filter(pattern -> !pattern.isEmpty())
                    .toArray(String[]::new));
        }
    }
}
