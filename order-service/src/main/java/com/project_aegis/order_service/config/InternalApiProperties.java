package com.project_aegis.order_service.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "internal.api")
public class InternalApiProperties {

    private String orderKey;
    private String inventoryKey;

}
