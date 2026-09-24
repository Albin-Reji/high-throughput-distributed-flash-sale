package com.project_aegis.order_service.client;

import com.project_aegis.order_service.client.dto.CustomerAddressClientResponse;
import com.project_aegis.order_service.dto.response.ApiResponse;
import com.project_aegis.order_service.exception.ResourceNotFoundException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.UUID;

@Slf4j
@Service
@CircuitBreaker(name = "userService")
public class UserServiceClient {

    private final RestClient userRestClient;

    public UserServiceClient(@Qualifier("userRestClient") RestClient userRestClient) {
        this.userRestClient = userRestClient;
    }

    public CustomerAddressClientResponse getAddress(UUID addressId, String bearerToken) {
        var requestSpec = userRestClient.get()
                .uri("/api/v1/customers/me/addresses/{addressId}", addressId);

        if (bearerToken != null && !bearerToken.isBlank()) {
            requestSpec.header(HttpHeaders.AUTHORIZATION, bearerToken);
        }

        ApiResponse<CustomerAddressClientResponse> response = requestSpec
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});

        if (response != null && response.getData() != null) {
            return response.getData();
        }

        throw new ResourceNotFoundException("Customer address not found: " + addressId);
    }
}
