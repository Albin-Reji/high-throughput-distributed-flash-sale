
package com.project_aegis.order_service.client;

import com.project_aegis.order_service.client.dto.StockDeductClientRequest;
import com.project_aegis.order_service.client.dto.StockReleaseClientRequest;
import com.project_aegis.order_service.client.dto.StockReservationClientRequest;
import com.project_aegis.order_service.config.InternalApiProperties;
import com.project_aegis.order_service.exception.StockReservationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Slf4j
@Service
public class InventoryServiceClient {

    private static final String INTERNAL_API_KEY_HEADER = "X-Internal-Api-Key";

    private final RestClient inventoryRestClient;
    private final InternalApiProperties internalApiProperties;

    public InventoryServiceClient(
            InternalApiProperties internalApiProperties,
            @Qualifier("inventoryRestClient") RestClient inventoryRestClient
    ) {
        this.internalApiProperties = internalApiProperties;
        this.inventoryRestClient = inventoryRestClient;
    }

    public void reserveStock(StockReservationClientRequest request) {
        try {
            inventoryRestClient.post()
                    .uri("/api/v1/inventory/internal/reserve")
                    .header(INTERNAL_API_KEY_HEADER, internalApiProperties.getInventoryKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .toBodilessEntity();

            log.info("Stock reserved successfully for orderId: {}", request.getOrderId());
        } catch (Exception ex) {
            log.warn(
                    "Failed to call inventory service to reserve stock for orderId: {}: {}",
                    request.getOrderId(),
                    ex.getMessage()
            );
            throw new StockReservationException(
                    "Failed to reserve stock for orderId: " + request.getOrderId() , ex
            );
        }
    }

    public void releaseStock(StockReleaseClientRequest request) {
        try {
            inventoryRestClient.post()
                    .uri("/api/v1/inventory/internal/release")
                    .header(INTERNAL_API_KEY_HEADER, internalApiProperties.getInventoryKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .toBodilessEntity();

            log.info("Stock released successfully for orderId: {}", request.getOrderId());
        } catch (Exception ex) {
            log.warn(
                    "Failed to call inventory service to release stock for orderId: {}: {}",
                    request.getOrderId(),
                    ex.getMessage()
            );
            throw new StockReservationException(
                    "Failed to release stock for orderId: " + request.getOrderId() ,ex
            );
        }
    }

    public void decrementStock(StockDeductClientRequest request) {
        try {
            inventoryRestClient.post()
                    .uri("/api/v1/inventory/internal/decrement")
                    .header(INTERNAL_API_KEY_HEADER, internalApiProperties.getInventoryKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .toBodilessEntity();

            log.info(
                    "Stock decremented successfully for orderId: {}",
                    request.getOrderId()
            );
        } catch (Exception ex) {
            log.warn(
                    "Failed to call inventory service to decrement stock for orderId: {}: {}",
                    request.getOrderId(),
                    ex.getMessage()
            );
            throw new StockReservationException(
                    "Failed to decrement stock for orderId: " + request.getOrderId() , ex
            );
        }
    }
}
