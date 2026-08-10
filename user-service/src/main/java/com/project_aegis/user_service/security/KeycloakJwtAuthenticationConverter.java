package com.project_aegis.user_service.security;

import org.jspecify.annotations.NonNull;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
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
@Component
public class KeycloakJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final JwtAuthenticationConverter converter = new JwtAuthenticationConverter();

    public KeycloakJwtAuthenticationConverter() {

        converter.setJwtGrantedAuthoritiesConverter(jwt -> {

            Map<String, Object> realmAccess =
                    jwt.getClaim("realm_access");

            if (realmAccess == null) {
                return List.of();
            }

            Object rolesObject = realmAccess.get("roles");

            if (!(rolesObject instanceof Collection<?> roles)) {
                return List.of();
            }

            return roles.stream()
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .map(role -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + role))
                    .toList();
        });
    }

    @Override
    public AbstractAuthenticationToken convert(@NonNull Jwt jwt) {
        return converter.convert(jwt);
    }
}