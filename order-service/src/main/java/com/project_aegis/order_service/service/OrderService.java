package com.project_aegis.order_service.service;

import com.project_aegis.order_service.dto.request.CreateOrderRequest;
import com.project_aegis.order_service.dto.response.CreateOrderResponse;
import com.project_aegis.order_service.dto.response.OrderDetailResponse;
import com.project_aegis.order_service.dto.response.OrderSummaryResponse;
import com.project_aegis.order_service.dto.response.PageResponse;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface OrderService {

    CreateOrderResponse createOrder(UUID customerId, String idempotencyKey, CreateOrderRequest request, String bearerToken);

    PageResponse<OrderSummaryResponse> getCustomerOrders(UUID customerId, Pageable pageable);

    OrderDetailResponse getOrderDetail(UUID customerId, UUID orderId, boolean isAdmin);

    OrderSummaryResponse cancelOrder(UUID customerId, UUID orderId);
}
