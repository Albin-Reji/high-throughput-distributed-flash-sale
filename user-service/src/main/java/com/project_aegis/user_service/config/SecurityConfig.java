package com.project_aegis.user_service.config;

import com.project_aegis.user_service.security.KeycloakJwtAuthenticationConverter;
import com.project_aegis.user_service.security.SecurityAccessDeniedHandler;
import com.project_aegis.user_service.security.SecurityAuthenticationEntryPoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final SecurityAuthenticationEntryPoint securityAuthenticationEntryPoint;
    private final SecurityAccessDeniedHandler securityAccessDeniedHandler;

    public SecurityConfig(SecurityAuthenticationEntryPoint securityAuthenticationEntryPoint, SecurityAccessDeniedHandler securityAccessDeniedHandler) {
        this.securityAuthenticationEntryPoint = securityAuthenticationEntryPoint;
        this.securityAccessDeniedHandler = securityAccessDeniedHandler;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,
                                            KeycloakJwtAuthenticationConverter keycloakJwtAuthenticationConverter) {

        http
                // Stateless REST API using Bearer JWT authentication.
                // Authentication credentials are supplied explicitly via the Authorization header,
                // so CSRF protection is not required.
                .csrf(AbstractHttpConfigurer::disable)
                // Session creation policy for stateless auth
                .sessionManagement(session -> session.sessionCreationPolicy(
                        SessionCreationPolicy.STATELESS
                ))
                // disabling the processing of cors in this service
                // gateway process the cors
                .cors(AbstractHttpConfigurer::disable)

                .authorizeHttpRequests(auth -> auth

                        // Public endpoints
                        .requestMatchers("/actuator/health",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**")
                        .permitAll()
                        // metrics only accessible with ROLE_SRE
                        .requestMatchers("/actuator/metrics/**", "/actuator/**")
                        .hasRole("SRE")
                        // Internal webhook endpoints (secured by API key, not JWT)
                        .requestMatchers("/internal/**")
                        .permitAll()
                        // Public Api w/o Authentication
                        .requestMatchers("/api/v1/public/**")
                        .permitAll()
                        // ADMIN endpoints
                        .requestMatchers("/api/v1/admin/**")
                        .hasRole("ADMIN")
                        // Customer endpoints should be authenticated
                        .requestMatchers("/api/v1/customers/**")
                        .authenticated()
                        // Everything else requires authentication
                        .anyRequest()
                        .authenticated())

                // OAuth2 Resource Server
                .oauth2ResourceServer(
                        oauth2 -> oauth2.jwt(
                                        jwt -> jwt.jwtAuthenticationConverter(
                                                keycloakJwtAuthenticationConverter
                                        )
                                )
//                       Unauthorized (401) handling
                                .authenticationEntryPoint(securityAuthenticationEntryPoint)
//                         Forbidden (403) handling
                                .accessDeniedHandler(securityAccessDeniedHandler)
                );

        return http.build();
    }


}