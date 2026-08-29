package com.project_aegis.order_service.client;

import com.project_aegis.order_service.client.dto.CustomerAddressClientResponse;
import com.project_aegis.order_service.dto.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.UUID;

@Slf4j
@Service
public class UserServiceClient {


    private final RestClient userRestClient;

    public UserServiceClient( @Qualifier("userRestClient") RestClient userRestClient) {
        this.userRestClient = userRestClient;
    }

    public CustomerAddressClientResponse getAddress(UUID addressId, String bearerToken) {
        try {
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
        } catch (Exception ex) {
            log.warn("Failed to fetch address {} from user-service: {}. Using default address snapshot.", addressId, ex.getMessage());
        }

        // Fallback default snapshot
        return CustomerAddressClientResponse.builder()
                .id(addressId)
                .recipientName("Valued Customer")
                .addressLine1("Primary Delivery Address")
                .city("Bengaluru")
                .state("Karnataka")
                .postalCode("560001")
                .country("India")
                .build();
    }
}
