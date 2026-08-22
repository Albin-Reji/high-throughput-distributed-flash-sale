package com.project_aegis.inventory_service.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for internal service-to-service API communication.
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
     * Shared secret key used to authenticate internal service calls
     * (e.g. from Order Service via X-Internal-Api-Key).
     */
    private String key;
}
