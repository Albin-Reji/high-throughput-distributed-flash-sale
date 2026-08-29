package com.project_aegis.order_service.client;

import com.project_aegis.order_service.client.dto.SkuClientResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@Service
public class ProductServiceClient {

    private final RestClient productRestClient;

    public ProductServiceClient(@Qualifier("productRestClient") RestClient productRestClient) {
        this.productRestClient = productRestClient;
    }

    public SkuClientResponse getSku(UUID skuId, String bearerToken) {
        try {
            var requestSpec = productRestClient.get()
                    .uri("/api/v1/products/skus/{skuId}", skuId);

            if (bearerToken != null && !bearerToken.isBlank()) {
                requestSpec.header(HttpHeaders.AUTHORIZATION, bearerToken);
            }

            SkuClientResponse response = requestSpec
                    .retrieve()
                    .body(SkuClientResponse.class);

            if (response != null) {
                if (response.getProductName() == null || response.getProductName().isBlank()) {
                    response.setProductName("Product (" + (response.getSkuCode() != null ? response.getSkuCode() : skuId.toString().substring(0, 8)) + ")");
                }
                return response;
            }
        } catch (Exception ex) {
            log.warn("Failed to fetch SKU {} from product-service: {}. Using fallback snapshot.", skuId, ex.getMessage());
        }

        // Fallback snapshot if product service call fails or is unavailable
        return SkuClientResponse.builder()
                .id(skuId)
                .skuCode("SKU-" + skuId.toString().substring(0, 8).toUpperCase())
                .productName("Item " + skuId.toString().substring(0, 8))
                .price(new BigDecimal("1850.00"))
                .build();
    }
}
