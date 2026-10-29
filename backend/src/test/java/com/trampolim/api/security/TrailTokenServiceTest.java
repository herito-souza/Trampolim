package com.trampolim.api.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class TrailTokenServiceTest {
    private TrailTokenService tokens;

    @BeforeEach
    void setUp() {
        tokens = new TrailTokenService();
        ReflectionTestUtils.setField(tokens, "oportunidadesSecret", "opportunities-test-secret-0123456789-abcdefghijklmnopqrstuvwxyz");
        ReflectionTestUtils.setField(tokens, "empresasSecret", "company-test-secret-0123456789-abcdefghijklmnopqrstuvwxyz");
        ReflectionTestUtils.setField(tokens, "capacitaSecret", "education-test-secret-0123456789-abcdefghijklmnopqrstuvwxyz");
    }

    @Test
    void tokenIsAcceptedOnlyByTheTrailThatIssuedIt() {
        String companyToken = tokens.issue(TrailTokenService.Trail.EMPRESAS, "corp@example.com", 42L, 3);

        var verified = tokens.verify(companyToken, TrailTokenService.Trail.EMPRESAS);

        assertNotNull(verified);
        assertEquals("corp@example.com", verified.getSubject());
        assertEquals(42L, verified.getClaim("uid").asLong());
        assertEquals(3, verified.getClaim("ver").asInt());
        assertEquals("empresas", verified.getClaim("trail").asString());
        assertNull(tokens.verify(companyToken, TrailTokenService.Trail.CAPACITA));
        assertNull(tokens.verify(companyToken, TrailTokenService.Trail.OPORTUNIDADES));
    }

    @Test
    void differentTrailAccountsCanUseTheSameEmailWithoutSharingTokens() {
        String email = "same@example.com";
        String opportunitiesToken = tokens.issue(TrailTokenService.Trail.OPORTUNIDADES, email, 1L, 0);
        String studentToken = tokens.issue(TrailTokenService.Trail.CAPACITA, email, 1L, 0);

        assertNotNull(tokens.verify(opportunitiesToken, TrailTokenService.Trail.OPORTUNIDADES));
        assertNotNull(tokens.verify(studentToken, TrailTokenService.Trail.CAPACITA));
        assertNull(tokens.verify(opportunitiesToken, TrailTokenService.Trail.CAPACITA));
        assertNull(tokens.verify(studentToken, TrailTokenService.Trail.OPORTUNIDADES));
    }
    @Test
    void rejectsTokensWithoutExpirationEvenWhenSignatureAndTrailAreValid() {
        String token = com.auth0.jwt.JWT.create().withIssuer("trampolim-empresas-api")
                .withAudience("trampolim-empresas-client").withSubject("corp@example.test")
                .withClaim("trail", "empresas").withClaim("uid", 1L).withClaim("ver", 0)
                .withIssuedAt(java.time.Instant.now())
                .sign(com.auth0.jwt.algorithms.Algorithm.HMAC256("company-test-secret-0123456789-abcdefghijklmnopqrstuvwxyz"));
        assertNull(tokens.verify(token, TrailTokenService.Trail.EMPRESAS));
    }

    @Test
    void refusesSharedSigningKeysAtStartup() {
        tokens.validateConfiguration();
        ReflectionTestUtils.setField(tokens, "capacitaSecret", "company-test-secret-0123456789-abcdefghijklmnopqrstuvwxyz");
        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class, tokens::validateConfiguration);
    }
    @Test
    void productionRejectsPublicDevelopmentAndExampleKeysWithoutPrintingThem() {
        var environment=new org.springframework.mock.env.MockEnvironment();
        environment.setActiveProfiles("prod");
        ReflectionTestUtils.setField(tokens,"environment",environment);
        tokens.validateConfiguration();
        for(String field:java.util.List.of("oportunidadesSecret","empresasSecret","capacitaSecret")) {
            Object original=ReflectionTestUtils.getField(tokens,field);
            for(String example:java.util.List.of("TRAMPOLIM_LOCAL_DEV_SECRET_CHANGE_ME_2026","gere-um-segredo-aleatorio-com-pelo-menos-32-bytes")) {
                ReflectionTestUtils.setField(tokens,field,example);
                var error=org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class,tokens::validateConfiguration);
                org.junit.jupiter.api.Assertions.assertFalse(error.getMessage().contains(example));
            }
            ReflectionTestUtils.setField(tokens,field,original);
        }
    }
}
