package com.aegis.product_service.config;

import com.aegis.product_service.security.KeycloakJwtAuthenticationConverter;
import com.aegis.product_service.security.SecurityAccessDeniedHandler;
import com.aegis.product_service.security.SecurityAuthenticationEntryPoint;
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

                        .requestMatchers("/api/v1/admin/**")
                        .hasRole("ADMIN")


                        .requestMatchers("/api/v1/products/**",
                                "/api/v1/categories/**")
                        .hasAnyRole("USER", "ADMIN")
//                     admin privilege apis

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
