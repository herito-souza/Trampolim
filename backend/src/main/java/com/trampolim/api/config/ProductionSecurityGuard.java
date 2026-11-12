package com.trampolim.api.config;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

/** Stops unsafe production settings before any datasource/migration is initialized. Never prints values. */
@Component
public class ProductionSecurityGuard implements InitializingBean {
    private final Environment env;
    public ProductionSecurityGuard(Environment env) { this.env = env; }
    @Override public void afterPropertiesSet() {
        if (!env.acceptsProfiles(Profiles.of("prod", "production"))) return;
        String origins = value("app.cors.allowed-origins");
        require(!origins.isBlank(), "app.cors.allowed-origins");
        for (String origin : origins.split(",", -1)) https(origin.trim(), true, "app.cors.allowed-origins");
        require(value("app.cors.allowed-origin-patterns").isBlank(), "app.cors.allowed-origin-patterns");
        https(value("app.frontend-url"), false, "app.frontend-url");
        require("validate".equals(value("spring.jpa.hibernate.ddl-auto")), "spring.jpa.hibernate.ddl-auto");
        require(flag("spring.flyway.enabled", false), "spring.flyway.enabled");
        require(!flag("spring.flyway.baseline-on-migrate", true), "spring.flyway.baseline-on-migrate");
        require(flag("spring.flyway.clean-disabled", true), "spring.flyway.clean-disabled");
        for (String name : List.of("trails.auth.legacy-token.enabled", "trails.auth.legacy-company.enabled", "trails.auth.legacy-organizations.enabled"))
            require(!flag(name, false), name);
        Set<String> users = new HashSet<>();
        for (String prefix : List.of("spring.datasource", "trampolim.datasource.empresas", "trampolim.datasource.capacita")) {
            if (!prefix.equals("spring.datasource")) require(flag(prefix + ".enabled", false), prefix + ".enabled");
            String username = value(prefix + ".username");
            require(!username.isBlank() && !Set.of("root", "postgres").contains(username.toLowerCase(Locale.ROOT)) && users.add(username.toLowerCase(Locale.ROOT)), prefix + ".username");
            String password = value(prefix + ".password");
            require(password.getBytes(StandardCharsets.UTF_8).length >= 16 && !Set.of("dev_empresas_only", "dev_capacita_only").contains(password) && !password.toLowerCase(Locale.ROOT).contains("troque-") && !password.startsWith("REPLACE_"), prefix + ".password");
        }
        Set<String> secrets = new HashSet<>();
        for (String key : List.of("api.security.token.secret", "trails.auth.empresas.secret", "trails.auth.capacita.secret", "operations.monitoring-key", "radar.bridge-secret")) {
            String secret = value(key);
            require(secret.getBytes(StandardCharsets.UTF_8).length >= 32 && secrets.add(secret) && !secret.toUpperCase(Locale.ROOT).startsWith("REPLACE_") && !secret.toUpperCase(Locale.ROOT).contains("CHANGE_ME") && !secret.startsWith("gere-") && !secret.startsWith("troque-"), key);
        }
        require(flag("app.mail.enabled", false), "app.mail.enabled");
        if (flag("app.mail.enabled", false)) {
            require(!value("spring.mail.host").isBlank(), "spring.mail.host");
            boolean tls = flag("spring.mail.properties.mail.smtp.ssl.enable", false)
                    || flag("spring.mail.properties.mail.smtp.starttls.enable", false) && flag("spring.mail.properties.mail.smtp.starttls.required", false);
            require(tls, "spring.mail.properties.mail.smtp.starttls.required");
        }
    }
    private String value(String name) { return env.getProperty(name, "").trim(); }
    private boolean flag(String name, boolean fallback) { return env.getProperty(name, Boolean.class, fallback); }
    private static void https(String value, boolean originOnly, String property) {
        try {
            URI uri = URI.create(value);
            require("https".equalsIgnoreCase(uri.getScheme()) && uri.getHost() != null && !uri.getHost().contains("*")
                    && uri.getRawUserInfo() == null && uri.getRawQuery() == null && uri.getRawFragment() == null
                    && (!originOnly || uri.getRawPath() == null || uri.getRawPath().isEmpty()), property);
        } catch (IllegalArgumentException invalid) { throw unsafe(property); }
    }
    private static void require(boolean allowed, String property) { if (!allowed) throw unsafe(property); }
    private static IllegalStateException unsafe(String property) { return new IllegalStateException("Configuração de produção insegura: " + property + ". Consulte o checklist de implantação."); }
}
