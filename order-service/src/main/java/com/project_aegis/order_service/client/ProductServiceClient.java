package com.project_aegis.order_service.client;

import com.project_aegis.order_service.client.dto.SkuClientResponse;
import com.project_aegis.order_service.exception.ProductServiceClientException;
import com.project_aegis.order_service.exception.ResourceNotFoundException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.UUID;

@Slf4j
@Service
@CircuitBreaker(name = "productService")
public class ProductServiceClient {

    private final RestClient productRestClient;

    public ProductServiceClient(@Qualifier("productRestClient") RestClient productRestClient) {
        this.productRestClient = productRestClient;
    }

    public SkuClientResponse getSku(UUID skuId, String bearerToken) {
        var requestSpec = productRestClient.get()
                .uri("/api/v1/products/skus/{skuId}", skuId);

        if (bearerToken != null && !bearerToken.isBlank()) {
            requestSpec.header(HttpHeaders.AUTHORIZATION, bearerToken);
        }

        SkuClientResponse response = requestSpec
                .retrieve()
                .body(SkuClientResponse.class);

        if (response == null) {
            throw new ResourceNotFoundException("SKU not found with ID: " + skuId);
        }

        if (response.getPrice() == null) {
            throw new ProductServiceClientException("Product service returned no price for SKU: " + skuId);
        }

        if (response.getProductName() == null || response.getProductName().isBlank()) {
            response.setProductName("Product (" + (response.getSkuCode() != null ? response.getSkuCode() : skuId.toString().substring(0, 8)) + ")");
        }

        return response;
    }
}
