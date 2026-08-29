package com.project_aegis.order_service.controller.admin;

import com.project_aegis.order_service.dto.request.AdminOrderStatusUpdateRequest;
import com.project_aegis.order_service.dto.response.OrderSummaryResponse;
import com.project_aegis.order_service.dto.response.PageResponse;
import com.project_aegis.order_service.entity.OrderStatus;
import com.project_aegis.order_service.service.AdminOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin/orders")
@RequiredArgsConstructor
@Tag(name = "Admin Orders", description = "Admin operations for order fulfillment, search, and status tracking")
public class AdminOrderController {

    private final AdminOrderService adminOrderService;

    @Operation(summary = "Global Search - Paginated search of all orders with optional status and customer filters")
    @GetMapping
    public ResponseEntity<PageResponse<OrderSummaryResponse>> searchOrders(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) UUID customerId,
            @PageableDefault(page = 0, size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(adminOrderService.searchOrders(status, customerId, pageable));
    }

    @Operation(summary = "Fulfillment Update - Update order status, tracking number, and shipping carrier")
    @PatchMapping("/{orderId}/status")
    public ResponseEntity<OrderSummaryResponse> updateOrderStatus(
            @PathVariable UUID orderId,
            @Valid @RequestBody AdminOrderStatusUpdateRequest request
    ) {
        return ResponseEntity.ok(adminOrderService.updateOrderStatus(orderId, request));
    }
}
