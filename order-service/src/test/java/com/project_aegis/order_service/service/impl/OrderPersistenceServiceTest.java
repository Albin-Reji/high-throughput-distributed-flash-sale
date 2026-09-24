package com.project_aegis.order_service.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project_aegis.order_service.dto.response.CreateOrderResponse;
import com.project_aegis.order_service.entity.IdempotencyRecord;
import com.project_aegis.order_service.entity.Order;
import com.project_aegis.order_service.entity.OrderStatus;
import com.project_aegis.order_service.entity.OutboxEvent;
import com.project_aegis.order_service.entity.OutboxStatus;
import com.project_aegis.order_service.mapper.OrderMapper;
import com.project_aegis.order_service.repository.IdempotencyRecordRepository;
import com.project_aegis.order_service.repository.OrderRepository;
import com.project_aegis.order_service.repository.OutboxEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderPersistenceService Unit Tests")
class OrderPersistenceServiceTest {

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private IdempotencyRecordRepository idempotencyRecordRepository;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private OrderPersistenceService orderPersistenceService;

    private UUID orderId;
    private UUID customerId;
    private Order order;
    private CreateOrderResponse createOrderResponse;

    @BeforeEach
    void setUp() {
        orderId = UUID.randomUUID();
        customerId = UUID.randomUUID();

        order = Order.builder()
                .id(orderId)
                .orderNumber("ORD-TEST-001")
                .customerId(customerId)
                .status(OrderStatus.PENDING)
                .totalAmount(BigDecimal.valueOf(500))
                .build();

        createOrderResponse = CreateOrderResponse.builder()
                .orderId(orderId)
                .orderNumber("ORD-TEST-001")
                .status(OrderStatus.AWAITING_PAYMENT)
                .totalAmount(BigDecimal.valueOf(500))
                .currency("INR")
                .build();
    }

    @Test
    @DisplayName("saveInitOrder should set status to PENDING and persist order")
    void saveInitOrder_ShouldSetPendingAndSave() {
        order.setStatus(OrderStatus.AWAITING_PAYMENT);
        when(orderRepository.save(order)).thenReturn(order);

        Order result = orderPersistenceService.saveInitOrder(order);

        assertThat(result.getStatus()).isEqualTo(OrderStatus.PENDING);
        verify(orderRepository).save(order);
    }

    @Test
    @DisplayName("confirmOrder should set AWAITING_PAYMENT, write outbox event, and record idempotency")
    void confirmOrder_ShouldConfirmAndPersistOutboxAndIdempotency() throws Exception {
        String idempotencyKey = "idemp-key-999";
        String serializedJson = "{\"orderId\":\"" + orderId + "\"}";

        when(orderRepository.save(order)).thenReturn(order);
        when(orderMapper.toCreateResponse(order)).thenReturn(createOrderResponse);
        when(objectMapper.writeValueAsString(createOrderResponse)).thenReturn(serializedJson);

        Order result = orderPersistenceService.confirmOrder(order, customerId, idempotencyKey);

        assertThat(result.getStatus()).isEqualTo(OrderStatus.AWAITING_PAYMENT);
        verify(orderRepository).save(order);

        // Verify Outbox Event
        ArgumentCaptor<OutboxEvent> outboxCaptor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(outboxCaptor.capture());
        OutboxEvent capturedOutbox = outboxCaptor.getValue();
        assertThat(capturedOutbox.getAggregateType()).isEqualTo("ORDER");
        assertThat(capturedOutbox.getAggregateId()).isEqualTo(orderId);
        assertThat(capturedOutbox.getEventType()).isEqualTo("ORDER_CREATED");
        assertThat(capturedOutbox.getPayload()).isEqualTo(serializedJson);
        assertThat(capturedOutbox.getStatus()).isEqualTo(OutboxStatus.PENDING);

        // Verify Idempotency Record
        ArgumentCaptor<IdempotencyRecord> idempCaptor = ArgumentCaptor.forClass(IdempotencyRecord.class);
        verify(idempotencyRecordRepository).save(idempCaptor.capture());
        IdempotencyRecord capturedIdemp = idempCaptor.getValue();
        assertThat(capturedIdemp.getIdempotencyKey()).isEqualTo(idempotencyKey);
        assertThat(capturedIdemp.getCustomerId()).isEqualTo(customerId);
        assertThat(capturedIdemp.getOrderId()).isEqualTo(orderId);
        assertThat(capturedIdemp.getResponseBody()).isEqualTo(serializedJson);
        assertThat(capturedIdemp.getStatusCode()).isEqualTo(201);
    }

    @Test
    @DisplayName("confirmOrder without idempotency key should skip saving idempotency record")
    void confirmOrder_WithoutIdempotencyKey_ShouldSkipIdempotency() throws Exception {
        when(orderRepository.save(order)).thenReturn(order);
        when(orderMapper.toCreateResponse(order)).thenReturn(createOrderResponse);
        when(objectMapper.writeValueAsString(createOrderResponse)).thenReturn("{}");

        Order result = orderPersistenceService.confirmOrder(order, customerId, null);

        assertThat(result.getStatus()).isEqualTo(OrderStatus.AWAITING_PAYMENT);
        verify(outboxEventRepository).save(any(OutboxEvent.class));
        verify(idempotencyRecordRepository, never()).save(any());
    }

    @Test
    @DisplayName("confirmOrder should throw IllegalStateException when serialization fails")
    void confirmOrder_SerializationFailure_ShouldThrow() throws Exception {
        when(orderRepository.save(order)).thenReturn(order);
        when(orderMapper.toCreateResponse(order)).thenReturn(createOrderResponse);
        when(objectMapper.writeValueAsString(createOrderResponse)).thenThrow(new RuntimeException("Serialization error"));

        assertThatThrownBy(() -> orderPersistenceService.confirmOrder(order, customerId, "key"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Failed to serialize outbox event payload");
    }

    @Test
    @DisplayName("failOrder should set status to FAILED and persist order")
    void failOrder_ShouldSetFailedAndSave() {
        order.setStatus(OrderStatus.PENDING);
        when(orderRepository.save(order)).thenReturn(order);

        orderPersistenceService.failOrder(order);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.FAILED);
        verify(orderRepository).save(order);
    }
}
