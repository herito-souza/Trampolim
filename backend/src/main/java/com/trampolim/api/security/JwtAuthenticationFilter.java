package com.trampolim.api.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import com.trampolim.api.user.AppUserRepository;
import java.util.Optional;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.dao.DataAccessException;

import java.io.IOException;
import java.util.Collections;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    @org.springframework.beans.factory.annotation.Value("${trails.auth.legacy-token.enabled:false}")
    private boolean legacyTokensEnabled;
    private final JwtUtil jwtUtil;
    private final AppUserRepository users;
    private final TrailTokenService trailTokens;
    private final ObjectProvider<JdbcTemplate> empresasJdbc;
    private final ObjectProvider<JdbcTemplate> capacitaJdbc;

    public JwtAuthenticationFilter(JwtUtil jwtUtil, AppUserRepository users, TrailTokenService trailTokens,
            @Qualifier("empresasJdbcTemplate") ObjectProvider<JdbcTemplate> empresasJdbc,
            @Qualifier("capacitaJdbcTemplate") ObjectProvider<JdbcTemplate> capacitaJdbc) {
        this.jwtUtil = jwtUtil;
        this.users = users;
        this.trailTokens = trailTokens;
        this.empresasJdbc = empresasJdbc;
        this.capacitaJdbc = capacitaJdbc;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        var token = this.recoverToken(request);
        if (token != null) {
            try {
                authenticateTrailToken(request, token);
            } catch (DataAccessException exception) {
                response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
                response.setContentType("application/json");
                response.setCharacterEncoding("UTF-8");
                response.getWriter().write("{\"error\":\"O serviço desta área está temporariamente indisponível.\"}");
                return;
            }
        }
        filterChain.doFilter(request, response);
    }

    private void authenticateTrailToken(HttpServletRequest request, String token) {
        String path = request.getRequestURI();
        boolean companyRoute = path.startsWith("/api/company/") || path.startsWith("/api/empresas/");
        boolean studentRoute = path.startsWith("/api/capacita/");
        if (companyRoute) {
            authenticateIndependent(token, TrailTokenService.Trail.EMPRESAS, empresasJdbc, "company_users");
            return;
        }
        if (studentRoute) {
            authenticateIndependent(token, TrailTokenService.Trail.CAPACITA, capacitaJdbc, "student_users");
            return;
        }

        var subject = legacyTokensEnabled ? jwtUtil.validateToken(token) : ""; // Legacy Oportunidades tokens remain valid only in this context.
        if (subject != null && !subject.isEmpty() && jwtUtil.getTokenVersion(token) >= 0 && users.findByEmail(subject)
                .filter(user -> user.getTokenVersion() == jwtUtil.getTokenVersion(token)).isPresent()) {
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(subject, null, Collections.emptyList()));
            return;
        }
        var decoded = trailTokens.verify(token, TrailTokenService.Trail.OPORTUNIDADES);
        if (decoded == null) return;
        int version = decoded.getClaim("ver").asInt();
        users.findByEmail(decoded.getSubject()).filter(user -> user.getTokenVersion() == version
                        && java.util.Objects.equals(user.getId(), decoded.getClaim("uid").asLong()))
                .ifPresent(user -> SecurityContextHolder.getContext().setAuthentication(
                        new UsernamePasswordAuthenticationToken(user.getEmail(), null, Collections.emptyList())));
    }

    private void authenticateIndependent(String token, TrailTokenService.Trail trail,
            ObjectProvider<JdbcTemplate> provider, String table) {
        var decoded = trailTokens.verify(token, trail);
        JdbcTemplate jdbc = provider.getIfAvailable();
        if (decoded == null || jdbc == null) return;
        long userId = decoded.getClaim("uid").asLong();
        int tokenVersion = decoded.getClaim("ver").asInt();
        String sql = "SELECT email FROM " + table + " WHERE id=? AND LOWER(email)=LOWER(?) AND token_version=? AND account_status='ACTIVE'";
        Optional<String> account = jdbc.query(sql, (rs, row) -> rs.getString(1), userId, decoded.getSubject(), tokenVersion)
                .stream().findFirst();
        account.ifPresent(email -> SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(email, null, Collections.emptyList())));
    }

    private String recoverToken(HttpServletRequest request) {
        var authHeader = request.getHeader("Authorization");
        if (authHeader == null) return null;
        return authHeader.replace("Bearer ", "");
    }
}
