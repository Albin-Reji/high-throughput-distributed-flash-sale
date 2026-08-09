package com.project_aegis.user_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                // REST API -> disable CSRF
                .csrf(AbstractHttpConfigurer::disable)

                .authorizeHttpRequests(auth -> auth

                        // Public endpoints
                        .requestMatchers("/actuator/health", "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()

                        // ADMIN endpoints
                        .requestMatchers("/api/v1/admin/customers/**").hasRole("ADMIN")

                        // Everything else requires authentication
                        .anyRequest().authenticated())

                // OAuth2 Resource Server
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(keycloakJwtAuthenticationConverter())));

        return http.build();
    }

    /**
     * Converts Keycloak realm_access.roles into
     * Spring Security ROLE_* authorities.
     * <p>
     * Keycloak:
     * <p>
     * "realm_access": {
     * "roles": ["ADMIN"]
     * }
     * <p>
     * becomes:
     * <p>
     * ROLE_ADMIN
     */
    @Bean
    Converter<Jwt, ? extends AbstractAuthenticationToken> keycloakJwtAuthenticationConverter() {

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();

        converter.setJwtGrantedAuthoritiesConverter(jwt -> {

            Map<String, Object> realmAccess = jwt.getClaim("realm_access");

            // No realm_access claim
            if (realmAccess == null) {
                return List.<GrantedAuthority>of();
            }

            Object rolesObject = realmAccess.get("roles");

            // No roles
            if (!(rolesObject instanceof Collection<?> roles)) {
                return List.<GrantedAuthority>of();
            }

            return roles.stream().filter(String.class::isInstance).map(String.class::cast).map(role -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + role)).toList();
        });

        return converter;
    }
}