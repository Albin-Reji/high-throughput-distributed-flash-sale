package com.project_aegis.order_service.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@Slf4j
public class RestClientConfig {

    @Value("${service.product.url:http://localhost:8082}")
    private String productServiceUrl;

    @Value("${service.user.url:http://localhost:8081}")
    private String userServiceUrl;

    @Value("${service.inventory.url:http://localhost:8083}")
    private String inventoryServiceUrl;


    @Bean("productRestClient")
    public RestClient productRestClient() {
        String url = productServiceUrl.trim();
        log.info("Product Service Url: [{}]", url);
        return RestClient.builder()
                .baseUrl(url)
                .build();
    }

    @Bean("userRestClient")
    public RestClient userRestClient() {
        String url = userServiceUrl.trim();
        log.info("User Service Url: [{}]", url);
        return RestClient.builder()
                .baseUrl(url)
                .build();
    }

    @Bean("inventoryRestClient")
    public RestClient inventoryRestClient() {
        String url = inventoryServiceUrl.trim();
        log.info("Inventory Service Url: [{}]", url);
        return RestClient.builder()
                .baseUrl(url)
                .build();
    }
}
