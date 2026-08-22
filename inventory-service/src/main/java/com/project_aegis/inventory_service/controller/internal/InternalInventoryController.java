package com.project_aegis.inventory_service.controller.internal;

import com.project_aegis.inventory_service.config.InternalApiProperties;
import com.project_aegis.inventory_service.dto.internal.request.StockDeductRequest;
import com.project_aegis.inventory_service.dto.internal.request.StockReleaseRequest;
import com.project_aegis.inventory_service.dto.internal.request.StockReservationRequest;
import com.project_aegis.inventory_service.dto.internal.response.StockCheckResponse;
import com.project_aegis.inventory_service.dto.internal.response.StockReservationResponse;
import com.project_aegis.inventory_service.dto.response.ApiResponse;
import com.project_aegis.inventory_service.service.InternalInventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Controller for internal service-to-service communication (e.g., Order Service).
 * Secured via {@code X-Internal-Api-Key} header.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/inventory/internal")
@RequiredArgsConstructor
@Tag(name = "Internal Inventory", description = "Internal APIs for Order Service stock reservation, release, and decrement")
public class InternalInventoryController {

    private final InternalInventoryService internalInventoryService;
    private final InternalApiProperties internalApiProperties;

    @Operation(summary = "Reserve stock for an order")
    @PostMapping("/reserve")
    public ResponseEntity<ApiResponse<StockReservationResponse>> reserveStock(
            @RequestHeader(value = "X-Internal-Api-Key", required = false) String apiKey,
            @Valid @RequestBody StockReservationRequest request) {

        validateApiKey(apiKey);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(internalInventoryService.reserveStock(request));
    }

    @Operation(summary = "Release reserved stock for cancelled/timed-out order")
    @PostMapping("/release")
    public ResponseEntity<ApiResponse<Void>> releaseStock(
            @RequestHeader(value = "X-Internal-Api-Key", required = false) String apiKey,
            @Valid @RequestBody StockReleaseRequest request) {

        validateApiKey(apiKey);
        return ResponseEntity.ok(internalInventoryService.releaseStock(request));
    }

    @Operation(summary = "Confirm and decrement reserved stock upon payment success")
    @PostMapping("/decrement")
    public ResponseEntity<ApiResponse<Void>> decrementStock(
            @RequestHeader(value = "X-Internal-Api-Key", required = false) String apiKey,
            @Valid @RequestBody StockDeductRequest request) {

        validateApiKey(apiKey);
        return ResponseEntity.ok(internalInventoryService.decrementStock(request));
    }

    @Operation(summary = "Check current stock level for a SKU")
    @GetMapping("/stock/{skuId}")
    public ResponseEntity<ApiResponse<StockCheckResponse>> checkStock(
            @RequestHeader(value = "X-Internal-Api-Key", required = false) String apiKey,
            @PathVariable UUID skuId) {

        validateApiKey(apiKey);
        return ResponseEntity.ok(internalInventoryService.checkStock(skuId));
    }

    private void validateApiKey(String apiKey) {
        String configuredKey = internalApiProperties.getKey();
        if (configuredKey != null && !configuredKey.isEmpty()) {
            if (apiKey == null || !configuredKey.equals(apiKey)) {
                log.warn("Internal API call rejected — invalid or missing API key");
                throw new com.project_aegis.inventory_service.exception.InvalidOperationException(
                        "Unauthorized: Invalid internal API key");
            }
        }
    }
}
