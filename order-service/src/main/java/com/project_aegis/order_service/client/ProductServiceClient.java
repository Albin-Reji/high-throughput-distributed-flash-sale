package com.project_aegis.order_service.client;

import com.project_aegis.order_service.client.dto.SkuClientResponse;
import com.project_aegis.order_service.exception.ProductServiceClientException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

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

            if (response == null) {
                log.warn("SKU {} not found in product-service. Using fallback snapshot.", skuId);
                throw new ProductServiceClientException("SKU not found in product-service");
            }
            if (response.getPrice() == null) {
                throw new ProductServiceClientException(
                        "Product service returned no price for SKU: " + skuId
                );
            }

            return response;
        } catch (ProductServiceClientException ex) {
            throw ex;

        } catch (Exception ex) {
            log.warn("Failed to fetch SKU {} from product-service: {}. Using fallback snapshot.",
                    skuId, ex.getMessage());
            throw new ProductServiceClientException(
                    "Failed to fetch SKU from product-service", ex
            );
        }

    }
}
