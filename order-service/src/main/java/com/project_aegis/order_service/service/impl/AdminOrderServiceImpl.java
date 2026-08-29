package com.project_aegis.order_service.service.impl;

import com.project_aegis.order_service.dto.request.AdminOrderStatusUpdateRequest;
import com.project_aegis.order_service.dto.response.OrderSummaryResponse;
import com.project_aegis.order_service.dto.response.PageResponse;
import com.project_aegis.order_service.entity.Order;
import com.project_aegis.order_service.entity.OrderStatus;
import com.project_aegis.order_service.entity.OutboxEvent;
import com.project_aegis.order_service.entity.OutboxStatus;
import com.project_aegis.order_service.exception.ResourceNotFoundException;
import com.project_aegis.order_service.mapper.OrderMapper;
import com.project_aegis.order_service.repository.OrderRepository;
import com.project_aegis.order_service.repository.OutboxEventRepository;
import com.project_aegis.order_service.service.AdminOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminOrderServiceImpl implements AdminOrderService {

    private final OrderRepository orderRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final OrderMapper orderMapper;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<OrderSummaryResponse> searchOrders(OrderStatus status, UUID customerId, Pageable pageable) {
        log.info("Admin searching orders - status: {}, customerId: {}, page: {}, size: {}", status, customerId, pageable.getPageNumber(), pageable.getPageSize());

        Page<Order> orderPage;
        if (status != null && customerId != null) {
            orderPage = orderRepository.findByCustomerIdAndStatus(customerId, status, pageable);
        } else if (status != null) {
            orderPage = orderRepository.findByStatus(status, pageable);
        } else if (customerId != null) {
            orderPage = orderRepository.findByCustomerId(customerId, pageable);
        } else {
            orderPage = orderRepository.findAll(pageable);
        }

        return PageResponse.from(orderPage.map(orderMapper::toSummaryResponse));
    }

    @Override
    @Transactional
    public OrderSummaryResponse updateOrderStatus(UUID orderId, AdminOrderStatusUpdateRequest request) {
        log.info("Admin updating orderId: {} to status: {}, tracking: {}, carrier: {}", orderId, request.getStatus(), request.getTrackingNumber(), request.getCarrier());

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: " + orderId));

        order.setStatus(request.getStatus());

        if (request.getTrackingNumber() != null && !request.getTrackingNumber().isBlank()) {
            order.setTrackingNumber(request.getTrackingNumber());
        }

        if (request.getCarrier() != null && !request.getCarrier().isBlank()) {
            order.setCarrier(request.getCarrier());
        }

        Order updatedOrder = orderRepository.save(order);

        // Generate Outbox Event
        String outboxPayload = "{}";
        try {
            outboxPayload = objectMapper.writeValueAsString(request);
        } catch (Exception e) {
            log.error("Failed to serialize admin update outbox payload", e);
        }

        OutboxEvent outboxEvent = OutboxEvent.builder()
                .aggregateType("ORDER")
                .aggregateId(orderId)
                .eventType("ORDER_STATUS_UPDATED_" + request.getStatus())
                .payload(outboxPayload)
                .status(OutboxStatus.PENDING)
                .build();
        outboxEventRepository.save(outboxEvent);

        return orderMapper.toSummaryResponse(updatedOrder);
    }
}
