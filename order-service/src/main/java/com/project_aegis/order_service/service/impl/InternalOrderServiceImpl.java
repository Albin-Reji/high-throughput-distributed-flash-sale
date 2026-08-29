package com.project_aegis.order_service.service.impl;

import com.project_aegis.order_service.client.InventoryServiceClient;
import com.project_aegis.order_service.client.dto.StockDeductClientRequest;
import com.project_aegis.order_service.config.InternalApiProperties;
import com.project_aegis.order_service.dto.request.PaymentNotificationRequest;
import com.project_aegis.order_service.dto.response.OrderSummaryResponse;
import com.project_aegis.order_service.entity.Order;
import com.project_aegis.order_service.entity.OrderStatus;
import com.project_aegis.order_service.entity.OutboxEvent;
import com.project_aegis.order_service.entity.OutboxStatus;
import com.project_aegis.order_service.exception.InvalidOperationException;
import com.project_aegis.order_service.exception.ResourceNotFoundException;
import com.project_aegis.order_service.mapper.OrderMapper;
import com.project_aegis.order_service.repository.OrderRepository;
import com.project_aegis.order_service.repository.OutboxEventRepository;
import com.project_aegis.order_service.service.InternalOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class InternalOrderServiceImpl implements InternalOrderService {

    private final OrderRepository orderRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final InventoryServiceClient inventoryServiceClient;
    private final InternalApiProperties internalApiProperties;
    private final OrderMapper orderMapper;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public OrderSummaryResponse processPayment(UUID orderId, PaymentNotificationRequest request, String apiKey) {
        log.info("Processing internal payment notification for orderId: {}, transactionId: {}, status: {}", orderId, request.getTransactionId(), request.getStatus());

        validateApiKey(apiKey);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: " + orderId));

        if ("SUCCESS".equalsIgnoreCase(request.getStatus())) {
            order.setStatus(OrderStatus.PAID);

            // Deduct stock permanently upon payment success
            inventoryServiceClient.decrementStock(StockDeductClientRequest.builder()
                    .orderId(orderId)
                    .build());

            // Generate Outbox Event
            String outboxPayload = "{}";
            try {
                outboxPayload = objectMapper.writeValueAsString(request);
            } catch (Exception e) {
                log.error("Failed to serialize payment outbox payload", e);
            }

            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .aggregateType("ORDER")
                    .aggregateId(orderId)
                    .eventType("ORDER_PAID")
                    .payload(outboxPayload)
                    .status(OutboxStatus.PENDING)
                    .build();
            outboxEventRepository.save(outboxEvent);
        } else {
            order.setStatus(OrderStatus.FAILED);
        }

        Order updatedOrder = orderRepository.save(order);
        return orderMapper.toSummaryResponse(updatedOrder);
    }

    private void validateApiKey(String apiKey) {
        String configuredKey = internalApiProperties.getOrderKey();

        if (!StringUtils.hasText(configuredKey)) {
            return;
        }

        if (!StringUtils.hasText(apiKey) || !configuredKey.equals(apiKey)) {
            log.warn("Internal API call rejected — invalid or missing API key");
            throw new InvalidOperationException("Unauthorized: Invalid internal API key");
        }
    }
}
