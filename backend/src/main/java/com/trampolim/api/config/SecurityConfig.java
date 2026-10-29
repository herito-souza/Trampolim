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
                        .requestMatchers(HttpMethod.POST, "/api/auth/logout").authenticated()
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/empresas/auth/**", "/api/capacita/auth/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/health").permitAll()
                        // Allow Spring's sanitized error response to be rendered after a failed
                        // authenticated request; otherwise the error dispatch can mask the real status.
                        .requestMatchers("/error").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/monitoring/client-errors").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/webhooks/mercadopago").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/empresas/session/logout", "/api/capacita/session/logout").authenticated()
                        .requestMatchers("/api/capacita/account/**").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/capacita/progress").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/capacita/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/public/profiles/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/public/companies/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/public/companies/**").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
