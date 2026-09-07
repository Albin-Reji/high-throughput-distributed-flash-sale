package com.project_aegis.order_service.service.impl;

import com.project_aegis.order_service.dto.request.AdminOrderStatusUpdateRequest;
import com.project_aegis.order_service.dto.response.OrderSummaryResponse;
import com.project_aegis.order_service.dto.response.PageResponse;
import com.project_aegis.order_service.entity.Order;
import com.project_aegis.order_service.entity.OrderStatus;
import com.project_aegis.order_service.entity.OrderType;
import com.project_aegis.order_service.exception.InvalidStateTransitionException;
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
            order.setStatus(OrderStatus.PROCESSING);
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
            order.setStatus(OrderStatus.CONFIRMED);
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
        @DisplayName("should allow same-status update for SHIPPED when updating tracking details")
        void shouldAllowSameStatusUpdateForShipped() throws Exception {
            order.setStatus(OrderStatus.SHIPPED);
            order.setTrackingNumber("OLD-TRK");
            order.setCarrier("DHL");

            AdminOrderStatusUpdateRequest request = AdminOrderStatusUpdateRequest.builder()
                    .status(OrderStatus.SHIPPED)
                    .trackingNumber("NEW-TRK")
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
            assertThat(order.getTrackingNumber()).isEqualTo("NEW-TRK");
            assertThat(order.getCarrier()).isEqualTo("FedEx");
        }

        @Test
        @DisplayName("should transition from AWAITING_PAYMENT to PAID")
        void shouldTransitionFromAwaitingPaymentToPaid() {
            order.setStatus(OrderStatus.AWAITING_PAYMENT);
            AdminOrderStatusUpdateRequest request = AdminOrderStatusUpdateRequest.builder()
                    .status(OrderStatus.PAID)
                    .build();

            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
            when(orderRepository.save(order)).thenReturn(order);
            when(orderMapper.toSummaryResponse(order)).thenReturn(summaryResponse);

            adminOrderService.updateOrderStatus(orderId, request);

            assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
        }

        @Test
        @DisplayName("should transition from PAID to CONFIRMED")
        void shouldTransitionFromPaidToConfirmed() {
            order.setStatus(OrderStatus.PAID);
            AdminOrderStatusUpdateRequest request = AdminOrderStatusUpdateRequest.builder()
                    .status(OrderStatus.CONFIRMED)
                    .build();

            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
            when(orderRepository.save(order)).thenReturn(order);
            when(orderMapper.toSummaryResponse(order)).thenReturn(summaryResponse);

            adminOrderService.updateOrderStatus(orderId, request);

            assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        }

        @Test
        @DisplayName("should transition from CONFIRMED to PROCESSING")
        void shouldTransitionFromConfirmedToProcessing() {
            order.setStatus(OrderStatus.CONFIRMED);
            AdminOrderStatusUpdateRequest request = AdminOrderStatusUpdateRequest.builder()
                    .status(OrderStatus.PROCESSING)
                    .build();

            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
            when(orderRepository.save(order)).thenReturn(order);
            when(orderMapper.toSummaryResponse(order)).thenReturn(summaryResponse);

            adminOrderService.updateOrderStatus(orderId, request);

            assertThat(order.getStatus()).isEqualTo(OrderStatus.PROCESSING);
        }

        @Test
        @DisplayName("should transition from PROCESSING to SHIPPED")
        void shouldTransitionFromProcessingToShipped() {
            order.setStatus(OrderStatus.PROCESSING);
            AdminOrderStatusUpdateRequest request = AdminOrderStatusUpdateRequest.builder()
                    .status(OrderStatus.SHIPPED)
                    .build();

            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
            when(orderRepository.save(order)).thenReturn(order);
            when(orderMapper.toSummaryResponse(order)).thenReturn(summaryResponse);

            adminOrderService.updateOrderStatus(orderId, request);

            assertThat(order.getStatus()).isEqualTo(OrderStatus.SHIPPED);
        }

        @Test
        @DisplayName("should transition from SHIPPED to DELIVERED")
        void shouldTransitionFromShippedToDelivered() {
            order.setStatus(OrderStatus.SHIPPED);
            AdminOrderStatusUpdateRequest request = AdminOrderStatusUpdateRequest.builder()
                    .status(OrderStatus.DELIVERED)
                    .build();

            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
            when(orderRepository.save(order)).thenReturn(order);
            when(orderMapper.toSummaryResponse(order)).thenReturn(summaryResponse);

            adminOrderService.updateOrderStatus(orderId, request);

            assertThat(order.getStatus()).isEqualTo(OrderStatus.DELIVERED);
        }

        @Test
        @DisplayName("should allow cancellation from AWAITING_PAYMENT, PAID, CONFIRMED, and PROCESSING")
        void shouldAllowCancellationFromPreFulfillmentStates() {
            for (OrderStatus preFulfillmentStatus : List.of(
                    OrderStatus.AWAITING_PAYMENT,
                    OrderStatus.PAID,
                    OrderStatus.CONFIRMED,
                    OrderStatus.PROCESSING)) {

                order.setStatus(preFulfillmentStatus);
                AdminOrderStatusUpdateRequest request = AdminOrderStatusUpdateRequest.builder()
                        .status(OrderStatus.CANCELLED)
                        .build();

                when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
                when(orderRepository.save(order)).thenReturn(order);
                when(orderMapper.toSummaryResponse(order)).thenReturn(summaryResponse);

                adminOrderService.updateOrderStatus(orderId, request);

                assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
            }
        }

        @Test
        @DisplayName("should throw InvalidStateTransitionException when jumping states (PAID to SHIPPED)")
        void shouldThrowOnSkippingStates() {
            order.setStatus(OrderStatus.PAID);
            AdminOrderStatusUpdateRequest request = AdminOrderStatusUpdateRequest.builder()
                    .status(OrderStatus.SHIPPED)
                    .build();

            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

            assertThatThrownBy(() -> adminOrderService.updateOrderStatus(orderId, request))
                    .isInstanceOf(InvalidStateTransitionException.class)
                    .hasMessageContaining("PAID")
                    .hasMessageContaining("SHIPPED");
        }

        @Test
        @DisplayName("should throw InvalidStateTransitionException on backward transition (DELIVERED to PENDING)")
        void shouldThrowOnBackwardTransitionFromDelivered() {
            order.setStatus(OrderStatus.DELIVERED);
            AdminOrderStatusUpdateRequest request = AdminOrderStatusUpdateRequest.builder()
                    .status(OrderStatus.PENDING)
                    .build();

            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

            assertThatThrownBy(() -> adminOrderService.updateOrderStatus(orderId, request))
                    .isInstanceOf(InvalidStateTransitionException.class)
                    .hasMessageContaining("DELIVERED")
                    .hasMessageContaining("PENDING");
        }

        @Test
        @DisplayName("should throw InvalidStateTransitionException on transition from terminal CANCELLED to SHIPPED")
        void shouldThrowOnTransitionFromCancelled() {
            order.setStatus(OrderStatus.CANCELLED);
            AdminOrderStatusUpdateRequest request = AdminOrderStatusUpdateRequest.builder()
                    .status(OrderStatus.SHIPPED)
                    .build();

            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

            assertThatThrownBy(() -> adminOrderService.updateOrderStatus(orderId, request))
                    .isInstanceOf(InvalidStateTransitionException.class)
                    .hasMessageContaining("CANCELLED")
                    .hasMessageContaining("SHIPPED");
        }

        @Test
        @DisplayName("should throw InvalidStateTransitionException on transition from terminal FAILED state")
        void shouldThrowOnTransitionFromFailed() {
            order.setStatus(OrderStatus.FAILED);
            AdminOrderStatusUpdateRequest request = AdminOrderStatusUpdateRequest.builder()
                    .status(OrderStatus.PAID)
                    .build();

            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

            assertThatThrownBy(() -> adminOrderService.updateOrderStatus(orderId, request))
                    .isInstanceOf(InvalidStateTransitionException.class)
                    .hasMessageContaining("FAILED");
        }

        @Test
        @DisplayName("should throw InvalidStateTransitionException when cancelling a SHIPPED order")
        void shouldThrowWhenCancellingShippedOrder() {
            order.setStatus(OrderStatus.SHIPPED);
            AdminOrderStatusUpdateRequest request = AdminOrderStatusUpdateRequest.builder()
                    .status(OrderStatus.CANCELLED)
                    .build();

            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

            assertThatThrownBy(() -> adminOrderService.updateOrderStatus(orderId, request))
                    .isInstanceOf(InvalidStateTransitionException.class)
                    .hasMessageContaining("SHIPPED")
                    .hasMessageContaining("CANCELLED");
        }

        @Test
        @DisplayName("should throw InvalidStateTransitionException on same-status transition for non-updatable states")
        void shouldThrowOnSameStatusTransitionForDelivered() {
            order.setStatus(OrderStatus.DELIVERED);
            AdminOrderStatusUpdateRequest request = AdminOrderStatusUpdateRequest.builder()
                    .status(OrderStatus.DELIVERED)
                    .build();

            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

            assertThatThrownBy(() -> adminOrderService.updateOrderStatus(orderId, request))
                    .isInstanceOf(InvalidStateTransitionException.class)
                    .hasMessageContaining("DELIVERED");
        }

        @Test
        @DisplayName("should throw InvalidStateTransitionException when target status is null")
        void shouldThrowWhenTargetStatusIsNull() {
            order.setStatus(OrderStatus.PAID);
            AdminOrderStatusUpdateRequest request = AdminOrderStatusUpdateRequest.builder()
                    .status(null)
                    .build();

            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

            assertThatThrownBy(() -> adminOrderService.updateOrderStatus(orderId, request))
                    .isInstanceOf(InvalidStateTransitionException.class)
                    .hasMessageContaining("must not be null");
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

