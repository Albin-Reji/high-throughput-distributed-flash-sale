package com.project_aegis.user_service.config;

import com.project_aegis.user_service.security.KeycloakJwtAuthenticationConverter;
import com.project_aegis.user_service.security.SecurityAccessDeniedHandler;
import com.project_aegis.user_service.security.SecurityAuthenticationEntryPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@RequiredArgsConstructor
@EnableWebSecurity
public class SecurityConfig {

    private final SecurityAuthenticationEntryPoint securityAuthenticationEntryPoint;
    private final SecurityAccessDeniedHandler securityAccessDeniedHandler;

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,
                                            KeycloakJwtAuthenticationConverter keycloakJwtAuthenticationConverter)
            throws Exception{

        http
                // REST API -> disable CSRF
                .csrf(AbstractHttpConfigurer::disable)

                .authorizeHttpRequests(auth -> auth

                        // Public endpoints
                        .requestMatchers("/actuator/health",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**").permitAll()

                        // Internal webhook endpoints (secured by API key, not JWT)
                        .requestMatchers("/internal/**").permitAll()

                        // ADMIN endpoints
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        // Customer endpoints should be authenticated
                        .requestMatchers("/api/v1/customer/**").authenticated()
                        // Everything else requires authentication
                        .anyRequest().authenticated())

                // OAuth2 Resource Server
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(
                                        jwt -> jwt.jwtAuthenticationConverter(
                                                keycloakJwtAuthenticationConverter))
//                       Unauthorized (401) handling
                                .authenticationEntryPoint(securityAuthenticationEntryPoint)
//                         Forbidden (403) handling
                                .accessDeniedHandler(securityAccessDeniedHandler)
                );

        return http.build();
    }



}