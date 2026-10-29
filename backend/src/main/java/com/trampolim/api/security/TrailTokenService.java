package com.trampolim.api.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** Issues and verifies credentials with a different signing key, issuer and audience per trail. */
@Service
public class TrailTokenService {
    public enum Trail {
        OPORTUNIDADES("oportunidades"), EMPRESAS("empresas"), CAPACITA("capacita");
        private final String key;
        Trail(String key) { this.key = key; }
    }

    @Value("${api.security.token.secret:TRAMPOLIM_LOCAL_DEV_SECRET_CHANGE_ME_2026}") private String oportunidadesSecret;
    @Value("${trails.auth.empresas.secret:TRAMPOLIM_EMPRESAS_LOCAL_SECRET_CHANGE_ME_2026}") private String empresasSecret;
    @Value("${trails.auth.capacita.secret:TRAMPOLIM_CAPACITA_LOCAL_SECRET_CHANGE_ME_2026}") private String capacitaSecret;
    @org.springframework.beans.factory.annotation.Autowired
    private org.springframework.core.env.Environment environment;

    @jakarta.annotation.PostConstruct
    void validateConfiguration() {
        java.util.Set<String> keys = new java.util.HashSet<>();
        for (Trail trail : Trail.values()) {
            String key=secret(trail);
            if(environment!=null && environment.acceptsProfiles(org.springframework.core.env.Profiles.of("prod","production"))
                    && java.util.Set.of("TRAMPOLIM_LOCAL_DEV_SECRET_CHANGE_ME_2026",
                        "TRAMPOLIM_EMPRESAS_LOCAL_SECRET_CHANGE_ME_2026",
                        "TRAMPOLIM_CAPACITA_LOCAL_SECRET_CHANGE_ME_2026",
                        "gere-um-segredo-aleatorio-com-pelo-menos-32-bytes",
                        "gere-um-segredo-aleatorio-exclusivo-com-pelo-menos-32-bytes",
                        "gere-outro-segredo-aleatorio-exclusivo-com-pelo-menos-32-bytes").contains(key))
                throw new IllegalStateException("Configure uma chave JWT exclusiva de produção para a trilha "+trail.key+"; valores públicos de exemplo não são permitidos.");
            if (!keys.add(key))
                throw new IllegalStateException("Cada trilha precisa de uma chave JWT exclusiva.");
        }
    }

    public String issue(Trail trail, String email, long userId, int tokenVersion) {
        String issuer = issuer(trail);
        return JWT.create()
                .withIssuer(issuer)
                .withAudience(audience(trail))
                .withSubject(email)
                .withClaim("trail", trail.key)
                .withClaim("uid", userId)
                .withClaim("ver", tokenVersion)
                .withIssuedAt(Instant.now())
                .withExpiresAt(Instant.now().plus(2, ChronoUnit.HOURS))
                .sign(Algorithm.HMAC256(secret(trail)));
    }

    public DecodedJWT verify(String token, Trail expectedTrail) {
        try {
            DecodedJWT jwt = JWT.require(Algorithm.HMAC256(secret(expectedTrail)))
                    .withIssuer(issuer(expectedTrail))
                    .withAudience(audience(expectedTrail))
                    .withClaim("trail", expectedTrail.key)
                    .build().verify(token);
            if (jwt.getSubject() == null || jwt.getSubject().isBlank() || jwt.getExpiresAt() == null
                    || jwt.getIssuedAt() == null || jwt.getClaim("uid").asLong() == null
                    || jwt.getClaim("uid").asLong() <= 0 || jwt.getClaim("ver").asInt() == null
                    || jwt.getClaim("ver").asInt() < 0) return null;
            return jwt;
        } catch (JWTVerificationException | IllegalArgumentException exception) {
            return null;
        }
    }

    private String issuer(Trail trail) { return "trampolim-" + trail.key + "-api"; }
    private String audience(Trail trail) { return "trampolim-" + trail.key + "-client"; }
    private String secret(Trail trail) {
        String value = switch (trail) {
            case OPORTUNIDADES -> oportunidadesSecret;
            case EMPRESAS -> empresasSecret;
            case CAPACITA -> capacitaSecret;
        };
        if (value == null || value.getBytes(StandardCharsets.UTF_8).length < 32)
            throw new IllegalStateException("A chave JWT da trilha " + trail.key + " precisa ter pelo menos 32 bytes.");
        return value;
    }
}
