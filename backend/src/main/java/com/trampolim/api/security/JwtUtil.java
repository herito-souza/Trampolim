package com.trampolim.api.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import jakarta.annotation.PostConstruct;

@Service
public class JwtUtil {

    // Idealmente deve vir do application.properties
    @Value("${api.security.token.secret:TRAMPOLIM_SECRET_KEY_123}")
    private String secret;

    @PostConstruct
    void validateSecret() {
        if (secret == null || secret.getBytes(java.nio.charset.StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("JWT_SECRET deve ter pelo menos 32 bytes.");
        }
    }

    public String generateToken(String username) {
        return generateToken(username, 0);
    }

    public String generateToken(String username, int tokenVersion) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            return JWT.create()
                    .withIssuer("trampolim-api")
                    .withSubject(username)
                    .withClaim("ver", tokenVersion)
                    .withExpiresAt(genExpirationDate())
                    .sign(algorithm);
        } catch (JWTCreationException exception) {
            throw new RuntimeException("Erro enquanto tentava gerar o token", exception);
        }
    }

    public int getTokenVersion(String token) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            Integer version = JWT.require(algorithm).withIssuer("trampolim-api").build().verify(token).getClaim("ver").asInt();
            return version == null ? 0 : version;
        } catch (JWTVerificationException exception) {
            return -1;
        }
    }

    public String validateToken(String token) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            return JWT.require(algorithm)
                    .withIssuer("trampolim-api")
                    .build()
                    .verify(token)
                    .getSubject();
        } catch (JWTVerificationException exception) {
            return "";
        }
    }

    private Instant genExpirationDate() {
        // Expira em 2 horas
        return Instant.now().plus(2, ChronoUnit.HOURS);
    }
}
