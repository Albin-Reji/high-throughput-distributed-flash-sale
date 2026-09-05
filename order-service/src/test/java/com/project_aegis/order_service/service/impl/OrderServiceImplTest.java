package com.project_aegis.order_service.service.impl;

import com.project_aegis.order_service.client.InventoryServiceClient;
import com.project_aegis.order_service.client.dto.CustomerAddressClientResponse;
import com.project_aegis.order_service.client.dto.SkuClientResponse;
import com.project_aegis.order_service.dto.request.CreateOrderRequest;
import com.project_aegis.order_service.dto.request.OrderItemRequest;
import com.project_aegis.order_service.dto.response.CreateOrderResponse;
import com.project_aegis.order_service.dto.response.OrderDetailResponse;
import com.project_aegis.order_service.dto.response.OrderSummaryResponse;
import com.project_aegis.order_service.dto.response.PageResponse;
import com.project_aegis.order_service.entity.IdempotencyRecord;
import com.project_aegis.order_service.entity.Order;
import com.project_aegis.order_service.entity.OrderStatus;
import com.project_aegis.order_service.entity.OrderType;
import com.project_aegis.order_service.entity.OutboxEvent;
import com.project_aegis.order_service.exception.InvalidOperationException;
import com.project_aegis.order_service.exception.InvalidStateTransitionException;
import com.project_aegis.order_service.exception.ResourceNotFoundException;
import com.project_aegis.order_service.mapper.OrderMapper;
import com.project_aegis.order_service.repository.IdempotencyRecordRepository;
import com.project_aegis.order_service.repository.OrderRepository;
import com.project_aegis.order_service.repository.OutboxEventRepository;
import com.project_aegis.order_service.service.OrderService;
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
@DisplayName("OrderServiceImpl Unit Tests")
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private IdempotencyRecordRepository idempotencyRecordRepository;

    @Mock
    private com.project_aegis.order_service.client.ProductServiceClient productServiceClient;

    @Mock
    private com.project_aegis.order_service.client.UserServiceClient userServiceClient;

    @Mock
    private InventoryServiceClient inventoryServiceClient;

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private OrderPersistenceService orderPersistenceService;

    @InjectMocks
    private OrderServiceImpl orderService;

    private UUID orderId;
    private UUID customerId;
    private Order order;

    @BeforeEach
    void setUp() {
        orderId = UUID.randomUUID();
        customerId = UUID.randomUUID();

        order = Order.builder()
                .id(orderId)
                .orderNumber("ORD-2026-TEST1234")
                .customerId(customerId)
                .status(OrderStatus.AWAITING_PAYMENT)
                .orderType(OrderType.REGULAR)
                .subtotalAmount(BigDecimal.valueOf(1000))
                .taxAmount(BigDecimal.ZERO)
                .shippingFee(BigDecimal.ZERO)
                .totalAmount(BigDecimal.valueOf(1000))
                .currency("INR")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    // ──────────────────────────────────────────────────────────────
    //  getCustomerOrders
    // ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getCustomerOrders")
    class GetCustomerOrders {

        @Test
        @DisplayName("should return paginated orders for customer")
        void shouldReturnPaginatedOrders() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Order> orderPage = new PageImpl<>(List.of(order), pageable, 1);

            OrderSummaryResponse summaryResponse = OrderSummaryResponse.builder()
                    .orderId(orderId)
                    .orderNumber("ORD-2026-TEST1234")
                    .status(OrderStatus.AWAITING_PAYMENT)
                    .totalAmount(BigDecimal.valueOf(1000))
                    .build();

            when(orderRepository.findByCustomerId(customerId, pageable)).thenReturn(orderPage);
            when(orderMapper.toSummaryResponse(order)).thenReturn(summaryResponse);

            PageResponse<OrderSummaryResponse> result =
                    orderService.getCustomerOrders(customerId, pageable);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().getFirst().getOrderNumber()).isEqualTo("ORD-2026-TEST1234");
        }

        @Test
        @DisplayName("should return empty page when no orders exist")
        void shouldReturnEmptyPage() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Order> emptyPage = new PageImpl<>(List.of(), pageable, 0);

            when(orderRepository.findByCustomerId(customerId, pageable)).thenReturn(emptyPage);

            PageResponse<OrderSummaryResponse> result =
                    orderService.getCustomerOrders(customerId, pageable);

            assertThat(result.getContent()).isEmpty();
        }
    }

    // ──────────────────────────────────────────────────────────────
    //  getOrderDetail
    // ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getOrderDetail")
    class GetOrderDetail {

        @Test
        @DisplayName("should return order detail for order owner")
        void shouldReturnDetailForOwner() {
            OrderDetailResponse detailResponse = OrderDetailResponse.builder()
                    .orderId(orderId)
                    .orderNumber("ORD-2026-TEST1234")
                    .customerId(customerId)
                    .status(OrderStatus.AWAITING_PAYMENT)
                    .build();

            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
            when(orderMapper.toDetailResponse(order)).thenReturn(detailResponse);

            OrderDetailResponse result = orderService.getOrderDetail(customerId, orderId, false);

            assertThat(result.getOrderId()).isEqualTo(orderId);
            assertThat(result.getOrderNumber()).isEqualTo("ORD-2026-TEST1234");
        }

        @Test
        @DisplayName("should allow admin to access any order")
        void shouldAllowAdminAccess() {
            UUID differentCustomerId = UUID.randomUUID();
            OrderDetailResponse detailResponse = OrderDetailResponse.builder()
                    .orderId(orderId)
                    .build();

            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
            when(orderMapper.toDetailResponse(order)).thenReturn(detailResponse);

            OrderDetailResponse result = orderService.getOrderDetail(differentCustomerId, orderId, true);

            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("should throw InvalidOperationException when non-owner tries to access")
        void shouldThrowWhenUnauthorized() {
            UUID differentCustomerId = UUID.randomUUID();
            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

            assertThatThrownBy(() -> orderService.getOrderDetail(differentCustomerId, orderId, false))
                    .isInstanceOf(InvalidOperationException.class)
                    .hasMessageContaining("not authorized");
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when order not found")
        void shouldThrowWhenNotFound() {
            when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.getOrderDetail(customerId, orderId, false))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ──────────────────────────────────────────────────────────────
    //  cancelOrder
    // ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("cancelOrder")
    class CancelOrder {

        @Test
        @DisplayName("should cancel order in PENDING status")
        void shouldCancelPendingOrder() throws Exception {
            order.setStatus(OrderStatus.PENDING);
            OrderSummaryResponse cancelledResponse = OrderSummaryResponse.builder()
                    .orderId(orderId)
                    .status(OrderStatus.CANCELLED)
                    .build();

            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
            when(orderMapper.toSummaryResponse(order)).thenReturn(cancelledResponse);
            when(objectMapper.writeValueAsString(any())).thenReturn("{}");

            OrderSummaryResponse result = orderService.cancelOrder(customerId, orderId);

            assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
            assertThat(result.getStatus()).isEqualTo(OrderStatus.CANCELLED);
            verify(outboxEventRepository).save(any(OutboxEvent.class));
            verify(inventoryServiceClient).releaseStock(any());
        }

        @Test
        @DisplayName("should cancel order in AWAITING_PAYMENT status")
        void shouldCancelAwaitingPaymentOrder() throws Exception {
            order.setStatus(OrderStatus.AWAITING_PAYMENT);
            OrderSummaryResponse cancelledResponse = OrderSummaryResponse.builder()
                    .orderId(orderId)
                    .status(OrderStatus.CANCELLED)
                    .build();

            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
            when(orderMapper.toSummaryResponse(order)).thenReturn(cancelledResponse);
            when(objectMapper.writeValueAsString(any())).thenReturn("{}");

            OrderSummaryResponse result = orderService.cancelOrder(customerId, orderId);

            assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
            verify(inventoryServiceClient).releaseStock(any());
        }

        @Test
        @DisplayName("should throw InvalidOperationException when customer does not own the order")
        void shouldThrowWhenNotOwner() {
            UUID differentCustomerId = UUID.randomUUID();
            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

            assertThatThrownBy(() -> orderService.cancelOrder(differentCustomerId, orderId))
                    .isInstanceOf(InvalidOperationException.class)
                    .hasMessageContaining("not authorized");
        }

        @Test
        @DisplayName("should throw InvalidStateTransitionException when order is SHIPPED")
        void shouldThrowWhenShipped() {
            order.setStatus(OrderStatus.SHIPPED);
            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

            assertThatThrownBy(() -> orderService.cancelOrder(customerId, orderId))
                    .isInstanceOf(InvalidStateTransitionException.class)
                    .hasMessageContaining("cannot be cancelled");
        }

        @Test
        @DisplayName("should throw InvalidStateTransitionException when order is DELIVERED")
        void shouldThrowWhenDelivered() {
            order.setStatus(OrderStatus.DELIVERED);
            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

            assertThatThrownBy(() -> orderService.cancelOrder(customerId, orderId))
                    .isInstanceOf(InvalidStateTransitionException.class);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when order not found")
        void shouldThrowWhenNotFound() {
            when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.cancelOrder(customerId, orderId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ──────────────────────────────────────────────────────────────
    //  createOrder
    // ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("createOrder")
    class CreateOrder {

        private String idempotencyKey;
        private String bearerToken;
        private UUID addressId;
        private UUID skuId;
        private CreateOrderRequest createRequest;
        private CustomerAddressClientResponse addressResponse;
        private SkuClientResponse skuResponse;
        private CreateOrderResponse createOrderResponse;

        @BeforeEach
        void setUp() {
            idempotencyKey = "idemp-key-123";
            bearerToken = "Bearer test-jwt-token";
            addressId = UUID.randomUUID();
            skuId = UUID.randomUUID();

            createRequest = CreateOrderRequest.builder()
                    .shippingAddressId(addressId)
                    .items(List.of(
                            OrderItemRequest.builder()
                                    .skuId(skuId)
                                    .quantity(2)
                                    .build()
                    ))
                    .build();

            addressResponse = CustomerAddressClientResponse.builder()
                    .id(addressId)
                    .recipientName("John Doe")
                    .addressLine1("123 Main St")
                    .city("Bengaluru")
                    .state("Karnataka")
                    .postalCode("560001")
                    .country("India")
                    .build();

            skuResponse = SkuClientResponse.builder()
                    .id(skuId)
                    .skuCode("SKU-PRO-001")
                    .productName("MacBook Pro")
                    .price(BigDecimal.valueOf(500))
                    .build();

            createOrderResponse = CreateOrderResponse.builder()
                    .orderId(orderId)
                    .orderNumber("ORD-2026-TEST1234")
                    .status(OrderStatus.AWAITING_PAYMENT)
                    .totalAmount(BigDecimal.valueOf(1000))
                    .currency("INR")
                    .build();
        }

        @Test
        @DisplayName("should return cached response when idempotency record exists")
        void shouldReturnCachedResponseWhenIdempotencyKeyExists() throws Exception {
            IdempotencyRecord record = IdempotencyRecord.builder()
                    .idempotencyKey(idempotencyKey)
                    .customerId(customerId)
                    .orderId(orderId)
                    .responseBody("{\"orderNumber\":\"ORD-2026-TEST1234\"}")
                    .statusCode(201)
                    .build();

            when(idempotencyRecordRepository.findByIdempotencyKeyAndCustomerId(idempotencyKey, customerId))
                    .thenReturn(Optional.of(record));
            when(objectMapper.readValue(record.getResponseBody(), CreateOrderResponse.class))
                    .thenReturn(createOrderResponse);

            CreateOrderResponse result = orderService.createOrder(customerId, idempotencyKey, createRequest, bearerToken);

            assertThat(result).isEqualTo(createOrderResponse);
            assertThat(result.getOrderNumber()).isEqualTo("ORD-2026-TEST1234");
            verify(userServiceClient, never()).getAddress(any(), any());
            verify(productServiceClient, never()).getSku(any(), any());
            verify(orderPersistenceService, never()).saveInitOrder(any());
            verify(inventoryServiceClient, never()).reserveStock(any());
            verify(orderPersistenceService, never()).confirmOrder(any(), any(), any());
        }

        @Test
        @DisplayName("should fallback to db lookup when deserializing cached response fails")
        void shouldFallbackWhenDeserializationFails() throws Exception {
            IdempotencyRecord record = IdempotencyRecord.builder()
                    .idempotencyKey(idempotencyKey)
                    .customerId(customerId)
                    .orderId(orderId)
                    .responseBody("malformed-json")
                    .statusCode(201)
                    .build();

            when(idempotencyRecordRepository.findByIdempotencyKeyAndCustomerId(idempotencyKey, customerId))
                    .thenReturn(Optional.of(record));
            when(objectMapper.readValue(record.getResponseBody(), CreateOrderResponse.class))
                    .thenThrow(new RuntimeException("JSON error"));
            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
            when(orderMapper.toCreateResponse(order)).thenReturn(createOrderResponse);

            CreateOrderResponse result = orderService.createOrder(customerId, idempotencyKey, createRequest, bearerToken);

            assertThat(result).isEqualTo(createOrderResponse);
            verify(orderRepository).findById(orderId);
            verify(orderMapper).toCreateResponse(order);
        }

        @Test
        @DisplayName("should successfully create order and reserve stock")
        void shouldCreateOrderSuccessfully() {
            when(idempotencyRecordRepository.findByIdempotencyKeyAndCustomerId(idempotencyKey, customerId))
                    .thenReturn(Optional.empty());
            when(userServiceClient.getAddress(addressId, bearerToken)).thenReturn(addressResponse);
            when(productServiceClient.getSku(skuId, bearerToken)).thenReturn(skuResponse);
            when(orderPersistenceService.saveInitOrder(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
            doNothing().when(inventoryServiceClient).reserveStock(any());
            when(orderPersistenceService.confirmOrder(any(Order.class), eq(customerId), eq(idempotencyKey)))
                    .thenReturn(order);
            when(orderMapper.toCreateResponse(order)).thenReturn(createOrderResponse);

            CreateOrderResponse result = orderService.createOrder(customerId, idempotencyKey, createRequest, bearerToken);

            assertThat(result).isNotNull();
            assertThat(result.getOrderId()).isEqualTo(orderId);
            verify(userServiceClient).getAddress(addressId, bearerToken);
            verify(productServiceClient).getSku(skuId, bearerToken);
            verify(orderPersistenceService).saveInitOrder(any(Order.class));
            verify(inventoryServiceClient).reserveStock(any());
            verify(orderPersistenceService).confirmOrder(any(Order.class), eq(customerId), eq(idempotencyKey));
        }

        @Test
        @DisplayName("should call failOrder when stock reservation throws exception")
        void shouldHandleStockReservationFailure() {
            when(idempotencyRecordRepository.findByIdempotencyKeyAndCustomerId(idempotencyKey, customerId))
                    .thenReturn(Optional.empty());
            when(userServiceClient.getAddress(addressId, bearerToken)).thenReturn(addressResponse);
            when(productServiceClient.getSku(skuId, bearerToken)).thenReturn(skuResponse);
            when(orderPersistenceService.saveInitOrder(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
            doThrow(new RuntimeException("Stock reservation failed"))
                    .when(inventoryServiceClient).reserveStock(any());
            when(orderPersistenceService.confirmOrder(any(Order.class), eq(customerId), eq(idempotencyKey)))
                    .thenReturn(order);
            when(orderMapper.toCreateResponse(order)).thenReturn(createOrderResponse);

            CreateOrderResponse result = orderService.createOrder(customerId, idempotencyKey, createRequest, bearerToken);

            assertThat(result).isNotNull();
            verify(orderPersistenceService).failOrder(any(Order.class));
        }

        @Test
        @DisplayName("should fail immediately when user service fails to resolve address")
        void shouldFailWhenUserServiceFails() {
            when(idempotencyRecordRepository.findByIdempotencyKeyAndCustomerId(idempotencyKey, customerId))
                    .thenReturn(Optional.empty());
            when(userServiceClient.getAddress(addressId, bearerToken))
                    .thenThrow(new ResourceNotFoundException("Address not found"));

            assertThatThrownBy(() -> orderService.createOrder(customerId, idempotencyKey, createRequest, bearerToken))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Address not found");

            verify(productServiceClient, never()).getSku(any(), any());
            verify(orderPersistenceService, never()).saveInitOrder(any());
            verify(inventoryServiceClient, never()).reserveStock(any());
        }

        @Test
        @DisplayName("should fail immediately when product service fails to resolve SKU")
        void shouldFailWhenProductServiceFails() {
            when(idempotencyRecordRepository.findByIdempotencyKeyAndCustomerId(idempotencyKey, customerId))
                    .thenReturn(Optional.empty());
            when(userServiceClient.getAddress(addressId, bearerToken)).thenReturn(addressResponse);
            when(productServiceClient.getSku(skuId, bearerToken))
                    .thenThrow(new RuntimeException("Product catalog unavailable"));

            assertThatThrownBy(() -> orderService.createOrder(customerId, idempotencyKey, createRequest, bearerToken))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Product catalog unavailable");

            verify(orderPersistenceService, never()).saveInitOrder(any());
            verify(inventoryServiceClient, never()).reserveStock(any());
        }
    }
}
