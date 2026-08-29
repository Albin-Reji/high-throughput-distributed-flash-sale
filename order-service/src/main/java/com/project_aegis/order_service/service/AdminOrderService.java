package com.project_aegis.order_service.service;

import com.project_aegis.order_service.dto.request.AdminOrderStatusUpdateRequest;
import com.project_aegis.order_service.dto.response.OrderSummaryResponse;
import com.project_aegis.order_service.dto.response.PageResponse;
import com.project_aegis.order_service.entity.OrderStatus;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface AdminOrderService {

    PageResponse<OrderSummaryResponse> searchOrders(OrderStatus status, UUID customerId, Pageable pageable);

    OrderSummaryResponse updateOrderStatus(UUID orderId, AdminOrderStatusUpdateRequest request);
}
