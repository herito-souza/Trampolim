package com.trampolim.api.config;

import java.net.URI;
import java.util.HashSet;
import java.util.Locale;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/** Rejects accidental reuse of a PostgreSQL database before serving requests. */
@Component
public class TrailDatabaseIsolationGuard implements InitializingBean {
    private final Environment environment;
    public TrailDatabaseIsolationGuard(Environment environment) { this.environment = environment; }
    @Override public void afterPropertiesSet() {
        var identities = new HashSet<String>();
        identities.add(identity(environment.getRequiredProperty("spring.datasource.url")));
        for (String trail : new String[]{"empresas", "capacita"}) {
            String prefix = "trampolim.datasource." + trail;
            if (!environment.getProperty(prefix + ".enabled", Boolean.class, false)) continue;
            if (!identities.add(identity(environment.getRequiredProperty(prefix + ".url"))))
                throw new IllegalStateException("As trilhas precisam de bancos PostgreSQL distintos. Revise as URLs de conexão.");
        }
    }
    static String identity(String jdbcUrl) {
        if (jdbcUrl == null || !jdbcUrl.startsWith("jdbc:postgresql://"))
            throw new IllegalStateException("Configure uma URL PostgreSQL explícita para cada trilha.");
        try {
            URI uri = URI.create(jdbcUrl.substring(5));
            String host = uri.getHost();
            if (host == null || uri.getPath() == null || uri.getPath().length() < 2)
                throw new IllegalArgumentException();
            host = host.toLowerCase(Locale.ROOT);
            if (host.equals("127.0.0.1") || host.equals("[::1]") || host.equals("::1")) host = "localhost";
            return host + ":" + (uri.getPort() == -1 ? 5432 : uri.getPort()) + uri.getPath();
        } catch (IllegalArgumentException failure) {
            throw new IllegalStateException("URL PostgreSQL inválida na configuração das trilhas.");
        }
    }
}
