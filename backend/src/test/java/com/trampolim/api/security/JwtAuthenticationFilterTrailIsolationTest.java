package com.trampolim.api.security;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.trampolim.api.user.AppUserRepository;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

class JwtAuthenticationFilterTrailIsolationTest {
    private JwtUtil legacyTokens;
    private AppUserRepository users;
    private TrailTokenService tokens;
    private JwtAuthenticationFilter filter;
    private ObjectProvider<JdbcTemplate> companies;
    private ObjectProvider<JdbcTemplate> students;
    private JdbcTemplate companyDatabase;
    private JdbcTemplate studentDatabase;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        tokens = new TrailTokenService();
        ReflectionTestUtils.setField(tokens, "oportunidadesSecret", "opportunities-test-secret-0123456789-abcdefghijklmnopqrstuvwxyz");
        ReflectionTestUtils.setField(tokens, "empresasSecret", "company-test-secret-0123456789-abcdefghijklmnopqrstuvwxyz");
        ReflectionTestUtils.setField(tokens, "capacitaSecret", "education-test-secret-0123456789-abcdefghijklmnopqrstuvwxyz");
        companyDatabase = mock(JdbcTemplate.class);
        studentDatabase = mock(JdbcTemplate.class);
        companies = providerWith(companyDatabase);
        students = providerWith(studentDatabase);
        legacyTokens = mock(JwtUtil.class);
        users = mock(AppUserRepository.class);
        filter = new JwtAuthenticationFilter(legacyTokens, users, tokens, companies, students);
    }

    @AfterEach
    void clearContext() { SecurityContextHolder.clearContext(); }

    @Test
    void companyTokenCannotAuthenticateCapacitaPrivateRoute() throws Exception {
        rejects(TrailTokenService.Trail.EMPRESAS, "/api/capacita/progress");
    }

    @Test
    void capacitaTokenCannotAuthenticateCompanyPrivateRoute() throws Exception {
        rejects(TrailTokenService.Trail.CAPACITA, "/api/company/dashboard");
    }

    @Test
    void opportunitiesTokenCannotAuthenticateCompanyPrivateRoute() throws Exception {
        rejects(TrailTokenService.Trail.OPORTUNIDADES, "/api/company/dashboard");
    }

    @Test
    void opportunitiesTokenCannotAuthenticateAssociationRouteOwnedByEmpresas() throws Exception {
        rejects(TrailTokenService.Trail.OPORTUNIDADES, "/api/empresas/association/organizations");
    }

    @Test
    void capacitaTokenCannotAuthenticateAssociationRouteOwnedByEmpresas() throws Exception {
        rejects(TrailTokenService.Trail.CAPACITA, "/api/empresas/association/organizations");
    }

    @Test
    void companyTokenCannotAuthenticateCapacitaLogoutRoute() throws Exception {
        rejects(TrailTokenService.Trail.EMPRESAS, "/api/capacita/session/logout");
    }

    @Test
    void studentTokenCannotAuthenticateCompanyLogoutRoute() throws Exception {
        rejects(TrailTokenService.Trail.CAPACITA, "/api/empresas/session/logout");
    }

    @Test
    void companyTokenCannotAuthenticateLegacyOpportunitiesOrganizationRoute() throws Exception {
        rejects(TrailTokenService.Trail.EMPRESAS, "/api/legacy/association/organizations");
    }

    @Test
    @SuppressWarnings("unchecked")
    void validCompanyTokenUsesOnlyTheCompaniesDatabase() throws Exception {
        doReturn(List.of("company@example.com")).when(companyDatabase)
                .query(anyString(), any(RowMapper.class), any(Object[].class));

        authenticates(TrailTokenService.Trail.EMPRESAS, "/api/company/dashboard");

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals("company@example.com", SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        verifyNoInteractions(studentDatabase);
    }

    @Test
    @SuppressWarnings("unchecked")
    void validStudentTokenUsesOnlyTheCapacitaDatabase() throws Exception {
        doReturn(List.of("student@example.com")).when(studentDatabase)
                .query(anyString(), any(RowMapper.class), any(Object[].class));

        authenticates(TrailTokenService.Trail.CAPACITA, "/api/capacita/progress");

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals("student@example.com", SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        verifyNoInteractions(companyDatabase);
    }

    @Test
    void legacyTokenRequiresExplicitCompatibilityFlag() throws Exception {
        var user = new com.trampolim.api.user.AppUser();
        user.setId(7L);
        user.setEmail("legacy@example.test");
        when(legacyTokens.validateToken("old-token")).thenReturn(user.getEmail());
        when(legacyTokens.getTokenVersion("old-token")).thenReturn(0);
        when(users.findByEmail(user.getEmail())).thenReturn(java.util.Optional.of(user));
        applyOpportunitiesToken("old-token");
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verifyNoInteractions(legacyTokens);
        ReflectionTestUtils.setField(filter, "legacyTokensEnabled", true);
        applyOpportunitiesToken("old-token");
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        verifyNoInteractions(companyDatabase, studentDatabase);
    }

    @Test
    void opportunitiesTokenRequiresMatchingUserIdAndSessionVersion() throws Exception {
        var user = new com.trampolim.api.user.AppUser();
        user.setId(7L);
        user.setEmail("candidate@example.test");
        when(users.findByEmail(user.getEmail())).thenReturn(java.util.Optional.of(user));
        applyOpportunitiesToken(tokens.issue(TrailTokenService.Trail.OPORTUNIDADES, user.getEmail(), 8L, 0));
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        applyOpportunitiesToken(tokens.issue(TrailTokenService.Trail.OPORTUNIDADES, user.getEmail(), 7L, 1));
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        applyOpportunitiesToken(tokens.issue(TrailTokenService.Trail.OPORTUNIDADES, user.getEmail(), 7L, 0));
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        verifyNoInteractions(legacyTokens, companyDatabase, studentDatabase);
    }

    private void applyOpportunitiesToken(String token) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/account/profile");
        request.addHeader("Authorization", "Bearer " + token);
        filter.doFilter(request, new MockHttpServletResponse(), (req, res) -> {});
    }
    @Test
    void independentAccountsCannotEnterGlobalAdministrationEvenWithSameEmail() throws Exception {
        String adminEmail = "administrator@example.test";
        var admin = new com.trampolim.api.user.AppUser();
        admin.setId(7L);
        admin.setEmail(adminEmail);
        when(users.findByEmail(adminEmail)).thenReturn(java.util.Optional.of(admin));
        for (var trail : List.of(TrailTokenService.Trail.EMPRESAS, TrailTokenService.Trail.CAPACITA)) {
            for (String path : List.of("/api/admin/saas/organizations", "/api/admin/capacita/legacy-migration/execute",
                    "/api/admin/empresas/legacy-migration/execute")) {
                SecurityContextHolder.clearContext();
                var request = new MockHttpServletRequest("POST", path);
                request.addHeader("Authorization", "Bearer " + tokens.issue(trail, adminEmail, 7L, 0));
                filter.doFilter(request, new MockHttpServletResponse(), (req, res) -> {});
                assertNull(SecurityContextHolder.getContext().getAuthentication());
            }
        }
        verifyNoInteractions(users, companyDatabase, studentDatabase, legacyTokens);
    }
    private void rejects(TrailTokenService.Trail issuedFor, String requestPath) throws Exception {
        String token = tokens.issue(issuedFor, "person@example.com", 7L, 0);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", requestPath);
        request.addHeader("Authorization", "Bearer " + token);
        filter.doFilter(request, new MockHttpServletResponse(), (req, res) -> {});
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    private void authenticates(TrailTokenService.Trail issuedFor, String requestPath) throws Exception {
        String email = issuedFor == TrailTokenService.Trail.EMPRESAS ? "company@example.com" : "student@example.com";
        String token = tokens.issue(issuedFor, email, 7L, 0);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", requestPath);
        request.addHeader("Authorization", "Bearer " + token);
        filter.doFilter(request, new MockHttpServletResponse(), (req, res) -> {});
    }

    @SuppressWarnings("unchecked")
    private ObjectProvider<JdbcTemplate> providerWith(JdbcTemplate jdbc) {
        ObjectProvider<JdbcTemplate> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(jdbc);
        return provider;
    }
}
