package com.project_aegis.order_service.client;

import com.project_aegis.order_service.client.dto.SkuClientResponse;
import com.project_aegis.order_service.exception.ProductServiceClientException;
import com.project_aegis.order_service.exception.ResourceNotFoundException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Collections;
import java.util.List;
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

    public List<SkuClientResponse> getSkusBatch(List<UUID> skuIds, String bearerToken) {
        if (skuIds == null || skuIds.isEmpty()) {
            return Collections.emptyList();
        }

        var requestSpec = productRestClient.post()
                .uri("/api/v1/products/skus/batch")
                .contentType(MediaType.APPLICATION_JSON)
                .body(skuIds);

        if (bearerToken != null && !bearerToken.isBlank()) {
            requestSpec.header(HttpHeaders.AUTHORIZATION, bearerToken);
        }

        List<SkuClientResponse> responses = requestSpec
                .retrieve()
                .body(new ParameterizedTypeReference<List<SkuClientResponse>>() {});

        if (responses == null) {
            return Collections.emptyList();
        }

        for (SkuClientResponse response : responses) {
            if (response.getPrice() == null) {
                throw new ProductServiceClientException("Product service returned no price for SKU: " + response.getId());
            }
            if (response.getProductName() == null || response.getProductName().isBlank()) {
                response.setProductName("Product (" + (response.getSkuCode() != null ? response.getSkuCode() : (response.getId() != null ? response.getId().toString().substring(0, 8) : "unknown")) + ")");
            }
        }

        return responses;
    }
}
