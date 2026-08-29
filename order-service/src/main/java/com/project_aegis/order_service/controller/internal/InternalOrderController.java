package com.project_aegis.order_service.controller.internal;

import com.project_aegis.order_service.dto.request.PaymentNotificationRequest;
import com.project_aegis.order_service.dto.response.OrderSummaryResponse;
import com.project_aegis.order_service.service.InternalOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/internal/orders")
@RequiredArgsConstructor
@Tag(name = "Internal Orders", description = "Internal mesh APIs for Payment Service and inter-service webhooks")
public class InternalOrderController {

    private final InternalOrderService internalOrderService;

    @Operation(summary = "Payment Callback - Update order status to PAID upon payment gateway webhook")
    @PatchMapping("/{orderId}/payment")
    public ResponseEntity<OrderSummaryResponse> processPayment(
            @RequestHeader(value = "X-Internal-Api-Key", required = false) String apiKey,
            @PathVariable UUID orderId,
            @Valid @RequestBody PaymentNotificationRequest request
    ) {
        return ResponseEntity.ok(internalOrderService.processPayment(orderId, request, apiKey));
    }
}
