package com.project_aegis.order_service.controller.customer;

import com.project_aegis.order_service.dto.request.CreateOrderRequest;
import com.project_aegis.order_service.dto.response.CreateOrderResponse;
import com.project_aegis.order_service.dto.response.OrderDetailResponse;
import com.project_aegis.order_service.dto.response.OrderSummaryResponse;
import com.project_aegis.order_service.dto.response.PageResponse;
import com.project_aegis.order_service.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Tag(name = "Customer Orders", description = "Customer self-service APIs for order placement, history, details, and cancellation")
public class CustomerOrderController {

    private final OrderService orderService;

    @Operation(summary = "Standard Checkout - Place a new order with idempotency protection")
    @PostMapping
    public ResponseEntity<CreateOrderResponse> createOrder(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateOrderRequest request,
            HttpServletRequest servletRequest
    ) {
        UUID customerId = UUID.fromString(Objects.requireNonNull(jwt.getSubject()));
        String bearerToken = servletRequest.getHeader(HttpHeaders.AUTHORIZATION);

        CreateOrderResponse response = orderService.createOrder(customerId, idempotencyKey, request, bearerToken);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Order History - Paginated list of customer orders")
    @GetMapping
    public ResponseEntity<PageResponse<OrderSummaryResponse>> getOrderHistory(
            @AuthenticationPrincipal Jwt jwt,
            @PageableDefault(page = 0, size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        UUID customerId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(orderService.getCustomerOrders(customerId, pageable));
    }

    @Operation(summary = "Order Snapshot - View immutable order details with items and shipping address")
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderDetailResponse> getOrderDetail(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID orderId
    ) {
        UUID customerId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(orderService.getOrderDetail(customerId, orderId, false));
    }

    @Operation(summary = "Cancel Order - Cancel a pending or awaiting-payment order")
    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<OrderSummaryResponse> cancelOrder(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID orderId
    ) {
        UUID customerId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(orderService.cancelOrder(customerId, orderId));
    }
}
