package com.project_aegis.inventory_service.config;

import com.project_aegis.inventory_service.security.KeycloakJwtAuthenticationConverter;
import com.project_aegis.inventory_service.security.SecurityAccessDeniedHandler;
import com.project_aegis.inventory_service.security.SecurityAuthenticationEntryPoint;
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
                // Session creation policy for stateless auth
                .sessionManagement(session -> session.sessionCreationPolicy(
                        SessionCreationPolicy.STATELESS
                ))
                // disabling the processing of cors in this service
                // gateway process the cors
                .cors(AbstractHttpConfigurer::disable)

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**")
                        .permitAll()
                        // admin privilege apis — requires ADMIN role
                        .requestMatchers("/api/v1/inventory/admin/**")
                        .hasRole("ADMIN")
                        // internal service-to-service endpoints (secured via X-Internal-Api-Key)
                        .requestMatchers("/api/v1/inventory/internal/**")
                        .permitAll()
                        // public campaign APIs — authenticated users (JWT required, any role)
                        .requestMatchers("/api/v1/inventory/campaigns/**")
                        .authenticated()

                        .anyRequest()
                        .authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(
                                jwt -> jwt.jwtAuthenticationConverter(keycloakJwtAuthenticationConverter))
                        .authenticationEntryPoint(securityAuthenticationEntryPoint)
                        .accessDeniedHandler(securityAccessDeniedHandler)


                );

        return http.build();

    }
}

