package com.project_aegis.order_service.service.impl;

import com.project_aegis.order_service.client.InventoryServiceClient;
import com.project_aegis.order_service.config.InternalApiProperties;
import com.project_aegis.order_service.dto.request.PaymentNotificationRequest;
import com.project_aegis.order_service.dto.response.OrderSummaryResponse;
import com.project_aegis.order_service.entity.Order;
import com.project_aegis.order_service.entity.OrderStatus;
import com.project_aegis.order_service.entity.OrderType;
import com.project_aegis.order_service.exception.InvalidOperationException;
import com.project_aegis.order_service.exception.ResourceNotFoundException;
import com.project_aegis.order_service.mapper.OrderMapper;
import com.project_aegis.order_service.repository.OrderRepository;
import com.project_aegis.order_service.repository.OutboxEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("InternalOrderServiceImpl Unit Tests")
class InternalOrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private InventoryServiceClient inventoryServiceClient;

    @Mock
    private InternalApiProperties internalApiProperties;

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private InternalOrderServiceImpl internalOrderService;

    private UUID orderId;
    private Order order;
    private OrderSummaryResponse summaryResponse;

    @BeforeEach
    void setUp() {
        orderId = UUID.randomUUID();

        order = Order.builder()
                .id(orderId)
                .orderNumber("ORD-2026-ABCD1234")
                .customerId(UUID.randomUUID())
                .status(OrderStatus.AWAITING_PAYMENT)
                .orderType(OrderType.REGULAR)
                .subtotalAmount(BigDecimal.valueOf(500))
                .totalAmount(BigDecimal.valueOf(500))
                .createdAt(Instant.now())
                .build();

        summaryResponse = OrderSummaryResponse.builder()
                .orderId(orderId)
                .orderNumber("ORD-2026-ABCD1234")
                .status(OrderStatus.PAID)
                .totalAmount(BigDecimal.valueOf(500))
                .build();
    }

    // ──────────────────────────────────────────────────────────────
    //  processPayment
    // ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("processPayment")
    class ProcessPayment {

        @Test
        @DisplayName("should set status to PAID and deduct stock on SUCCESS payment")
        void shouldSetPaidOnSuccess() throws Exception {
            PaymentNotificationRequest request = PaymentNotificationRequest.builder()
                    .transactionId("txn-001")
                    .status("SUCCESS")
                    .amountPaid(BigDecimal.valueOf(500))
                    .build();

            when(internalApiProperties.getOrderKey()).thenReturn("");
            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
            when(orderRepository.save(order)).thenReturn(order);
            when(orderMapper.toSummaryResponse(order)).thenReturn(summaryResponse);
            when(objectMapper.writeValueAsString(request)).thenReturn("{}");

            OrderSummaryResponse result = internalOrderService.processPayment(orderId, request, "");

            assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
            verify(inventoryServiceClient).decrementStock(any());
            verify(outboxEventRepository).save(any());
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("should set status to FAILED on non-SUCCESS payment")
        void shouldSetFailedOnNonSuccess() {
            PaymentNotificationRequest request = PaymentNotificationRequest.builder()
                    .transactionId("txn-002")
                    .status("FAILED")
                    .amountPaid(BigDecimal.valueOf(500))
                    .build();

            when(internalApiProperties.getOrderKey()).thenReturn("");
            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
            when(orderRepository.save(order)).thenReturn(order);
            when(orderMapper.toSummaryResponse(order)).thenReturn(summaryResponse);

            internalOrderService.processPayment(orderId, request, "");

            assertThat(order.getStatus()).isEqualTo(OrderStatus.FAILED);
            verify(inventoryServiceClient, never()).decrementStock(any());
            verify(outboxEventRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw InvalidOperationException when API key is invalid")
        void shouldThrowOnInvalidApiKey() {
            PaymentNotificationRequest request = PaymentNotificationRequest.builder()
                    .transactionId("txn-003")
                    .status("SUCCESS")
                    .amountPaid(BigDecimal.valueOf(500))
                    .build();

            when(internalApiProperties.getOrderKey()).thenReturn("correct-key");

            assertThatThrownBy(() -> internalOrderService.processPayment(orderId, request, "wrong-key"))
                    .isInstanceOf(InvalidOperationException.class)
                    .hasMessageContaining("Unauthorized");
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when order not found")
        void shouldThrowWhenOrderNotFound() {
            PaymentNotificationRequest request = PaymentNotificationRequest.builder()
                    .transactionId("txn-004")
                    .status("SUCCESS")
                    .amountPaid(BigDecimal.valueOf(500))
                    .build();

            when(internalApiProperties.getOrderKey()).thenReturn("");
            when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> internalOrderService.processPayment(orderId, request, ""))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining(orderId.toString());
        }

        @Test
        @DisplayName("should allow request when API key is not configured (empty)")
        void shouldAllowWhenNoApiKeyConfigured() throws Exception {
            PaymentNotificationRequest request = PaymentNotificationRequest.builder()
                    .transactionId("txn-005")
                    .status("SUCCESS")
                    .amountPaid(BigDecimal.valueOf(500))
                    .build();

            when(internalApiProperties.getOrderKey()).thenReturn("");
            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
            when(orderRepository.save(order)).thenReturn(order);
            when(orderMapper.toSummaryResponse(order)).thenReturn(summaryResponse);
            when(objectMapper.writeValueAsString(request)).thenReturn("{}");

            OrderSummaryResponse result = internalOrderService.processPayment(orderId, request, null);

            assertThat(result).isNotNull();
            assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
        }
    }
}
