package com.project_aegis.order_service.service.impl;

import com.project_aegis.order_service.dto.request.AdminOrderStatusUpdateRequest;
import com.project_aegis.order_service.dto.response.OrderSummaryResponse;
import com.project_aegis.order_service.dto.response.PageResponse;
import com.project_aegis.order_service.entity.Order;
import com.project_aegis.order_service.entity.OrderStatus;
import com.project_aegis.order_service.entity.OrderType;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminOrderServiceImpl Unit Tests")
class AdminOrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private AdminOrderServiceImpl adminOrderService;

    private UUID orderId;
    private UUID customerId;
    private Order order;
    private OrderSummaryResponse summaryResponse;

    @BeforeEach
    void setUp() {
        orderId = UUID.randomUUID();
        customerId = UUID.randomUUID();

        order = Order.builder()
                .id(orderId)
                .orderNumber("ORD-2026-ABCD1234")
                .customerId(customerId)
                .status(OrderStatus.PAID)
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
                .createdAt(Instant.now())
                .build();
    }

    // ──────────────────────────────────────────────────────────────
    //  searchOrders
    // ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("searchOrders")
    class SearchOrders {

        private Pageable pageable;

        @BeforeEach
        void setUp() {
            pageable = PageRequest.of(0, 10);
        }

        @Test
        @DisplayName("should search by status and customerId when both provided")
        void shouldSearchByStatusAndCustomerId() {
            Page<Order> orderPage = new PageImpl<>(List.of(order), pageable, 1);
            when(orderRepository.findByCustomerIdAndStatus(customerId, OrderStatus.PAID, pageable))
                    .thenReturn(orderPage);
            when(orderMapper.toSummaryResponse(order)).thenReturn(summaryResponse);

            PageResponse<OrderSummaryResponse> result =
                    adminOrderService.searchOrders(OrderStatus.PAID, customerId, pageable);

            assertThat(result.getContent()).hasSize(1);
            verify(orderRepository).findByCustomerIdAndStatus(customerId, OrderStatus.PAID, pageable);
        }

        @Test
        @DisplayName("should search by status only when customerId is null")
        void shouldSearchByStatusOnly() {
            Page<Order> orderPage = new PageImpl<>(List.of(order), pageable, 1);
            when(orderRepository.findByStatus(OrderStatus.PAID, pageable)).thenReturn(orderPage);
            when(orderMapper.toSummaryResponse(order)).thenReturn(summaryResponse);

            PageResponse<OrderSummaryResponse> result =
                    adminOrderService.searchOrders(OrderStatus.PAID, null, pageable);

            assertThat(result.getContent()).hasSize(1);
            verify(orderRepository).findByStatus(OrderStatus.PAID, pageable);
        }

        @Test
        @DisplayName("should search by customerId only when status is null")
        void shouldSearchByCustomerIdOnly() {
            Page<Order> orderPage = new PageImpl<>(List.of(order), pageable, 1);
            when(orderRepository.findByCustomerId(customerId, pageable)).thenReturn(orderPage);
            when(orderMapper.toSummaryResponse(order)).thenReturn(summaryResponse);

            PageResponse<OrderSummaryResponse> result =
                    adminOrderService.searchOrders(null, customerId, pageable);

            assertThat(result.getContent()).hasSize(1);
            verify(orderRepository).findByCustomerId(customerId, pageable);
        }

        @Test
        @DisplayName("should return all orders when no filters provided")
        void shouldReturnAllOrders() {
            Page<Order> orderPage = new PageImpl<>(List.of(order), pageable, 1);
            when(orderRepository.findAll(pageable)).thenReturn(orderPage);
            when(orderMapper.toSummaryResponse(order)).thenReturn(summaryResponse);

            PageResponse<OrderSummaryResponse> result =
                    adminOrderService.searchOrders(null, null, pageable);

            assertThat(result.getContent()).hasSize(1);
            verify(orderRepository).findAll(pageable);
        }
    }

    // ──────────────────────────────────────────────────────────────
    //  updateOrderStatus
    // ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("updateOrderStatus")
    class UpdateOrderStatus {

        @Test
        @DisplayName("should update status with tracking number and carrier")
        void shouldUpdateStatusWithTrackingInfo() throws Exception {
            AdminOrderStatusUpdateRequest request = AdminOrderStatusUpdateRequest.builder()
                    .status(OrderStatus.SHIPPED)
                    .trackingNumber("TRK-12345")
                    .carrier("FedEx")
                    .build();

            OrderSummaryResponse expectedResponse = OrderSummaryResponse.builder()
                    .orderId(orderId)
                    .orderNumber("ORD-2026-ABCD1234")
                    .status(OrderStatus.SHIPPED)
                    .totalAmount(BigDecimal.valueOf(500))
                    .build();

            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
            when(orderRepository.save(order)).thenReturn(order);
            when(orderMapper.toSummaryResponse(order)).thenReturn(expectedResponse);
            when(objectMapper.writeValueAsString(request)).thenReturn("{}");

            OrderSummaryResponse result = adminOrderService.updateOrderStatus(orderId, request);

            assertThat(result.getStatus()).isEqualTo(OrderStatus.SHIPPED);
            assertThat(order.getStatus()).isEqualTo(OrderStatus.SHIPPED);
            assertThat(order.getTrackingNumber()).isEqualTo("TRK-12345");
            assertThat(order.getCarrier()).isEqualTo("FedEx");
            verify(outboxEventRepository).save(any());
        }

        @Test
        @DisplayName("should not set tracking when tracking number is blank")
        void shouldNotSetBlankTracking() throws Exception {
            AdminOrderStatusUpdateRequest request = AdminOrderStatusUpdateRequest.builder()
                    .status(OrderStatus.PROCESSING)
                    .trackingNumber("")
                    .carrier("")
                    .build();

            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
            when(orderRepository.save(order)).thenReturn(order);
            when(orderMapper.toSummaryResponse(order)).thenReturn(summaryResponse);
            when(objectMapper.writeValueAsString(request)).thenReturn("{}");

            adminOrderService.updateOrderStatus(orderId, request);

            assertThat(order.getTrackingNumber()).isNull();
            assertThat(order.getCarrier()).isNull();
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when order not found")
        void shouldThrowWhenOrderNotFound() {
            AdminOrderStatusUpdateRequest request = AdminOrderStatusUpdateRequest.builder()
                    .status(OrderStatus.SHIPPED)
                    .build();

            when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> adminOrderService.updateOrderStatus(orderId, request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining(orderId.toString());
        }
    }
}
