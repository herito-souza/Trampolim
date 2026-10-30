package com.trampolim.api.config;

import com.trampolim.api.security.JwtAuthenticationFilter;

import lombok.RequiredArgsConstructor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.http.HttpMethod;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@RequiredArgsConstructor
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final org.springframework.beans.factory.ObjectProvider<com.trampolim.api.education.LearningAccess> learningAccess;
    private final org.springframework.beans.factory.ObjectProvider<com.trampolim.api.education.LibraryService> libraryService;
    private final org.springframework.beans.factory.ObjectProvider<com.trampolim.api.hub.HubAccess> hubAccess;
    @org.springframework.beans.factory.annotation.Value("${capacita.library.max-bytes:20971520}") private long libraryLimit;
    @org.springframework.beans.factory.annotation.Value("${capacita.questions.media-max-bytes:26214400}") private long discussionMediaLimit;

    @org.springframework.beans.factory.annotation.Value("${operations.monitoring-key:}") private String monitoringKey;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults()) // Vai utilizar as configs do CorsConfig (WebMvcConfigurer)
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Only the completion redispatch of an already-authorized SSE request is anonymous.
                        .requestMatchers(request -> request.getDispatcherType()==jakarta.servlet.DispatcherType.ASYNC && request.getRequestURI().equals("/api/capacita/learning/notifications/stream")).permitAll()
                        .requestMatchers(HttpMethod.GET,"/api/public/career/**","/api/public/skills/**","/api/public/skills","/api/public/capacita/radar/training","/api/public/hub/**","/api/public/capacita/hub/**","/api/public/capacita/passport/verify/**").permitAll()
                        .requestMatchers(HttpMethod.POST,"/api/integrations/career/**","/api/integrations/radar/award","/api/integrations/capacita/radar/evidence","/api/integrations/future/summary","/api/integrations/hub/candidates","/api/integrations/hub/learning","/api/integrations/capacita/passport/verify","/api/integrations/radar/invalidate").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/logout").authenticated()
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/empresas/auth/**", "/api/capacita/auth/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/health", "/actuator/health/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/actuator/prometheus").access((authentication, context) -> new org.springframework.security.authorization.AuthorizationDecision(com.trampolim.api.config.OperationalRequestFilter.validMonitoringKey(monitoringKey, context.getRequest().getHeader("X-Monitoring-Key"))))
                        .requestMatchers("/actuator/**").denyAll()
                        // Allow Spring's sanitized error response to be rendered after a failed
                        // authenticated request; otherwise the error dispatch can mask the real status.
                        .requestMatchers("/error").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/monitoring/client-errors").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/webhooks/mercadopago").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/empresas/session/logout", "/api/capacita/session/logout").authenticated()
                        .requestMatchers("/api/capacita/account/**").authenticated()
                        .requestMatchers(HttpMethod.GET,"/api/capacita/learning/competencies/shared/**").permitAll()
                        .requestMatchers(HttpMethod.GET,"/api/capacita/learning/discussion/media/play/**").permitAll()
                        .requestMatchers(HttpMethod.HEAD,"/api/capacita/learning/discussion/media/play/**").permitAll()
                        .requestMatchers(HttpMethod.GET,"/api/capacita/learning/media/**").permitAll()
                        .requestMatchers(HttpMethod.HEAD,"/api/capacita/learning/media/**").permitAll()
                        .requestMatchers("/api/capacita/learning/**").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/capacita/progress").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/capacita/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/public/profiles/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/public/companies/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/public/companies/**").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(new com.trampolim.api.education.LearningUploadFilter(learningAccess,discussionMediaLimit),JwtAuthenticationFilter.class)
                .addFilterAfter(new com.trampolim.api.hub.HubLogoUploadFilter(hubAccess),JwtAuthenticationFilter.class)
                .addFilterAfter(new com.trampolim.api.education.LibraryUploadFilter(libraryService,libraryLimit),JwtAuthenticationFilter.class);

        return http.build();
    }
}
