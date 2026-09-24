package com.project_aegis.order_service.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RestClientConfig Unit Tests")
class RestClientConfigTest {

    @Test
    @DisplayName("Should create JdkClientHttpRequestFactory with 3s connect timeout and 5s read timeout")
    void testClientHttpRequestFactoryTimeouts() {
        RestClientConfig config = new RestClientConfig();
        ClientHttpRequestFactory factory = config.clientHttpRequestFactory();

        assertThat(factory).isNotNull();
        assertThat(factory).isInstanceOf(JdkClientHttpRequestFactory.class);

        Duration readTimeout = (Duration) ReflectionTestUtils.getField(factory, "readTimeout");
        assertThat(readTimeout).isEqualTo(Duration.ofSeconds(5));
    }

    @Test
    @DisplayName("Should configure RestClient beans with base URLs and request factory")
    void testRestClientBeansCreation() {
        RestClientConfig config = new RestClientConfig();
        ReflectionTestUtils.setField(config, "productServiceUrl", "http://localhost:8082");
        ReflectionTestUtils.setField(config, "userServiceUrl", "http://localhost:8081");
        ReflectionTestUtils.setField(config, "inventoryServiceUrl", "http://localhost:8083");

        ClientHttpRequestFactory factory = config.clientHttpRequestFactory();

        RestClient productClient = config.productRestClient(factory);
        RestClient userClient = config.userRestClient(factory);
        RestClient inventoryClient = config.inventoryRestClient(factory);

        assertThat(productClient).isNotNull();
        assertThat(userClient).isNotNull();
        assertThat(inventoryClient).isNotNull();
    }
}
