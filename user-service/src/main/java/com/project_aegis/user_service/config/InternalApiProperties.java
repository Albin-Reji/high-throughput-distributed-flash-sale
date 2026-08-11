package com.project_aegis.user_service.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for internal API communication.
 *
 * <p>Maps to {@code internal.api.*} keys in application YAML / environment variables.</p>
 *
 * <pre>
 * internal:
 *   api:
 *     key: ${INTERNAL_API_KEY}
 * </pre>
 */
@Configuration
@ConfigurationProperties(prefix = "internal.api")
@Getter
@Setter
public class InternalApiProperties {

    /**
     * Shared secret key used to authenticate internal webhook calls
     * (e.g., from the Keycloak Event Listener SPI).
     */
    private String key;
}
