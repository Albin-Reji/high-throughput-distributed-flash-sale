package com.project_aegis.order_service.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
@Slf4j
public class RestClientConfig {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(3);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(5);

    @Value("${service.product.url:http://localhost:8082}")
    private String productServiceUrl;

    @Value("${service.user.url:http://localhost:8081}")
    private String userServiceUrl;

    @Value("${service.inventory.url:http://localhost:8083}")
    private String inventoryServiceUrl;

    private ClientHttpRequestFactory createRequestFactory() {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(READ_TIMEOUT);
        return requestFactory;
    }

    @Bean("productRestClient")
    public RestClient productRestClient() {
        String url = productServiceUrl.trim();
        log.info("Product Service Url: [{}]", url);
        return RestClient.builder()
                .baseUrl(url)
                .requestFactory(createRequestFactory())
                .build();
    }

    @Bean("userRestClient")
    public RestClient userRestClient() {
        String url = userServiceUrl.trim();
        log.info("User Service Url: [{}]", url);
        return RestClient.builder()
                .baseUrl(url)
                .requestFactory(createRequestFactory())
                .build();
    }

    @Bean("inventoryRestClient")
    public RestClient inventoryRestClient() {
        String url = inventoryServiceUrl.trim();
        log.info("Inventory Service Url: [{}]", url);
        return RestClient.builder()
                .baseUrl(url)
                .requestFactory(createRequestFactory())
                .build();
    }
}
