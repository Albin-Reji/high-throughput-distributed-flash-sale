package com.project_aegis.order_service.config;

import com.project_aegis.order_service.security.KeycloakJwtAuthenticationConverter;
import com.project_aegis.order_service.security.SecurityAccessDeniedHandler;
import com.project_aegis.order_service.security.SecurityAuthenticationEntryPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final SecurityAuthenticationEntryPoint securityAuthenticationEntryPoint;
    private final SecurityAccessDeniedHandler securityAccessDeniedHandler;
    private final KeycloakJwtAuthenticationConverter keycloakJwtAuthenticationConverter;

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) {
        http.csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .cors(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/actuator/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**"
                        ).permitAll()
                        // metrics only accessible with ROLE_SRE
                        .requestMatchers("/actuator/metrics/**", "/actuator/**")
                        .hasRole("SRE")
                        // Internal APIs - secured via X-Internal-Api-Key header
                        .requestMatchers("/api/v1/internal/**").permitAll()
                        // Admin APIs - requires ADMIN role
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        // Customer Orders APIs - requires authenticated JWT
                        .requestMatchers("/api/v1/orders/**").authenticated()
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(
                                jwt -> jwt.jwtAuthenticationConverter(keycloakJwtAuthenticationConverter))
                        .authenticationEntryPoint(securityAuthenticationEntryPoint)
                        .accessDeniedHandler(securityAccessDeniedHandler)
                );

        return http.build();
    }
}
