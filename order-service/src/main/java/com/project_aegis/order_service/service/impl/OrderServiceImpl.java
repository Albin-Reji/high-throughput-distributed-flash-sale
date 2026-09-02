package com.project_aegis.order_service.service.impl;

import com.project_aegis.order_service.client.InventoryServiceClient;
import com.project_aegis.order_service.client.ProductServiceClient;
import com.project_aegis.order_service.client.UserServiceClient;
import com.project_aegis.order_service.client.dto.*;
import com.project_aegis.order_service.dto.request.CreateOrderRequest;
import com.project_aegis.order_service.dto.request.OrderItemRequest;
import com.project_aegis.order_service.dto.response.CreateOrderResponse;
import com.project_aegis.order_service.dto.response.OrderDetailResponse;
import com.project_aegis.order_service.dto.response.OrderSummaryResponse;
import com.project_aegis.order_service.dto.response.PageResponse;
import com.project_aegis.order_service.entity.*;
import com.project_aegis.order_service.exception.InvalidOperationException;
import com.project_aegis.order_service.exception.InvalidStateTransitionException;
import com.project_aegis.order_service.exception.ResourceNotFoundException;
import com.project_aegis.order_service.mapper.OrderMapper;
import com.project_aegis.order_service.repository.IdempotencyRecordRepository;
import com.project_aegis.order_service.repository.OrderRepository;
import com.project_aegis.order_service.repository.OutboxEventRepository;
import com.project_aegis.order_service.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Year;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private static final String AGGREGATE_TYPE_ORDER = "ORDER";
    private static final String EVENT_ORDER_CANCELLED = "ORDER_CANCELLED";

    private static final String DEFAULT_RECIPIENT_NAME = "Customer";
    private static final String DEFAULT_ADDRESS_LINE_1 = "Address Line 1";
    private static final String DEFAULT_CITY = "City";
    private static final String DEFAULT_STATE = "State";
    private static final String DEFAULT_POSTAL_CODE = "000000";
    private static final String DEFAULT_COUNTRY = "Country";

    private static final String CANCELLATION_REASON = "Order cancelled by customer";

    private static final String DEFAULT_CURRENCY = "INR";
    private static final int ORDER_NUMBER_LENGTH = 8;

    private final OrderRepository orderRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final IdempotencyRecordRepository idempotencyRecordRepository;
    private final ProductServiceClient productServiceClient;
    private final UserServiceClient userServiceClient;
    private final InventoryServiceClient inventoryServiceClient;
    private final OrderMapper orderMapper;
    private final ObjectMapper objectMapper;
    private final OrderPersistenceService orderPersistenceService;

    @Override
    public CreateOrderResponse createOrder(
            UUID customerId,
            String idempotencyKey,
            CreateOrderRequest request,
            String bearerToken
    ) {
        log.info(
                "Creating order for customerId: {} with idempotencyKey: {}",
                customerId,
                idempotencyKey
        );

        Optional<CreateOrderResponse> cachedResponse =
                findCachedResponse(customerId, idempotencyKey);

        if (cachedResponse.isPresent()) {
            return cachedResponse.get();
        }

        CustomerAddressClientResponse address =
                userServiceClient.getAddress(
                        request.getShippingAddressId(),
                        bearerToken
                );

        String orderNumber = generateOrderNumber();

        OrderBuildResult orderBuildResult =
                buildOrder(request, customerId, address, bearerToken, orderNumber);

        Order order = orderBuildResult.order();
        Order savedOrder = orderPersistenceService.saveInitOrder(order);


         // if reserve stock throe an exception ,
         // OrderStatus={@link OrderStatus#FAILED} and return error response to client
        try{
            reserveStock(
                    savedOrder,
                    customerId,
                    orderBuildResult.reservationItems()
            );
        } catch (Exception e) {
            orderPersistenceService.failOrder(savedOrder);
            log.error(
                    "Stock reservation failed for orderId: {}. Failed order.",
                    savedOrder.getId(),
                    e
            );

        }
        // if reserveStock() is success then confirm the order
        Order confirmOrder=orderPersistenceService.confirmOrder(savedOrder, customerId, idempotencyKey);

        return orderMapper.toCreateResponse(confirmOrder);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<OrderSummaryResponse> getCustomerOrders(
            UUID customerId,
            Pageable pageable
    ) {
        log.info(
                "Fetching orders for customerId: {} with page: {}, size: {}",
                customerId,
                pageable.getPageNumber(),
                pageable.getPageSize()
        );

        Page<Order> orderPage =
                orderRepository.findByCustomerId(customerId, pageable);

        return PageResponse.from(
                orderPage.map(orderMapper::toSummaryResponse)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public OrderDetailResponse getOrderDetail(
            UUID customerId,
            UUID orderId,
            boolean isAdmin
    ) {
        log.info(
                "Fetching order detail for orderId: {}, customerId: {}, isAdmin: {}",
                orderId,
                customerId,
                isAdmin
        );

        Order order = findOrder(orderId);

        validateOrderAccess(order, customerId, isAdmin);

        return orderMapper.toDetailResponse(order);
    }

    @Override
    @Transactional
    public OrderSummaryResponse cancelOrder(
            UUID customerId,
            UUID orderId
    ) {
        log.info(
                "Cancelling orderId: {} for customerId: {}",
                orderId,
                customerId
        );

        Order order = findOrder(orderId);

        validateOrderOwnership(order, customerId);
        validateCancellationState(order);

        order.setStatus(OrderStatus.CANCELLED);

        createCancellationOutboxEvent(order);

        releaseStock(orderId);

        return orderMapper.toSummaryResponse(order);
    }

    private Optional<CreateOrderResponse> findCachedResponse(
            UUID customerId,
            String idempotencyKey
    ) {
        if (!StringUtils.hasText(idempotencyKey)) {
            return Optional.empty();
        }

        Optional<IdempotencyRecord> records =
                idempotencyRecordRepository
                        .findByIdempotencyKeyAndCustomerId(
                                idempotencyKey,
                                customerId
                        );

        if (records.isEmpty()) {
            return Optional.empty();
        }

        log.info(
                "Duplicate request detected for idempotencyKey: {}",
                idempotencyKey
        );

        return deserializeCachedResponse(records.get());
    }

    private Optional<CreateOrderResponse> deserializeCachedResponse(
            IdempotencyRecord records
    ) {
        try {
            CreateOrderResponse response =
                    objectMapper.readValue(
                            records.getResponseBody(),
                            CreateOrderResponse.class
                    );

            return Optional.of(response);
        } catch (Exception ex) {
            log.warn(
                    "Failed to deserialize cached idempotency response for orderId: {}. Re-querying order.",
                    records.getOrderId(),
                    ex
            );

            Order order = findOrder(records.getOrderId());

            return Optional.of(orderMapper.toCreateResponse(order));
        }
    }

    private OrderBuildResult buildOrder(
            CreateOrderRequest request,
            UUID customerId,
            CustomerAddressClientResponse address,
            String bearerToken,
            String orderNumber
    ) {
        List<OrderItem> orderItems = new ArrayList<>();
        List<ReservationItemClientRequest> reservationItems =
                new ArrayList<>();

        BigDecimal subtotalAmount = BigDecimal.ZERO;

        for (OrderItemRequest itemRequest : request.getItems()) {
            SkuClientResponse sku =
                    productServiceClient.getSku(
                            itemRequest.getSkuId(),
                            bearerToken
                    );

            BigDecimal unitPrice = resolveUnitPrice(sku);
            BigDecimal itemSubtotal =
                    unitPrice.multiply(
                            BigDecimal.valueOf(itemRequest.getQuantity())
                    );

            subtotalAmount = subtotalAmount.add(itemSubtotal);

            orderItems.add(
                    buildOrderItem(
                            itemRequest,
                            sku,
                            unitPrice,
                            itemSubtotal
                    )
            );

            reservationItems.add(
                    ReservationItemClientRequest.builder()
                            .skuId(itemRequest.getSkuId())
                            .quantity(itemRequest.getQuantity())
                            .build()
            );
        }

        Order order = buildOrderEntity(
                orderNumber,
                customerId,
                subtotalAmount
        );

        orderItems.forEach(order::addItem);

        order.setShippingAddress(
                buildShippingAddress(
                        request.getShippingAddressId(),
                        address
                )
        );

        return new OrderBuildResult(order, reservationItems);
    }

    private OrderItem buildOrderItem(
            OrderItemRequest itemRequest,
            SkuClientResponse sku,
            BigDecimal unitPrice,
            BigDecimal itemSubtotal
    ) {
        return OrderItem.builder()
                .skuId(itemRequest.getSkuId())
                .skuCode(sku.getSkuCode())
                .productName(resolveProductName(sku))
                .quantity(itemRequest.getQuantity())
                .unitPrice(unitPrice)
                .subtotal(itemSubtotal)
                .build();
    }

    private Order buildOrderEntity(
            String orderNumber,
            UUID customerId,
            BigDecimal subtotalAmount
    ) {
        BigDecimal taxAmount = BigDecimal.ZERO;
        BigDecimal shippingFee = BigDecimal.ZERO;

        BigDecimal totalAmount =
                subtotalAmount
                        .add(taxAmount)
                        .add(shippingFee);

        return Order.builder()
                .orderNumber(orderNumber)
                .customerId(customerId)
                .status(OrderStatus.AWAITING_PAYMENT)
                .orderType(OrderType.REGULAR)
                .subtotalAmount(subtotalAmount)
                .taxAmount(taxAmount)
                .shippingFee(shippingFee)
                .totalAmount(totalAmount)
                .currency(DEFAULT_CURRENCY)
                .build();
    }

    private OrderShippingAddress buildShippingAddress(
            UUID addressId,
            CustomerAddressClientResponse address
    ) {
        return OrderShippingAddress.builder()
                .originalAddressId(addressId)
                .recipientName(
                        defaultIfBlank(
                                address.getRecipientName(),
                                DEFAULT_RECIPIENT_NAME
                        )
                )
                .addressLine1(
                        defaultIfBlank(
                                address.getAddressLine1(),
                                DEFAULT_ADDRESS_LINE_1
                        )
                )
                .addressLine2(address.getAddressLine2())
                .city(
                        defaultIfBlank(
                                address.getCity(),
                                DEFAULT_CITY
                        )
                )
                .state(
                        defaultIfBlank(
                                address.getState(),
                                DEFAULT_STATE
                        )
                )
                .postalCode(
                        defaultIfBlank(
                                address.getPostalCode(),
                                DEFAULT_POSTAL_CODE
                        )
                )
                .country(
                        defaultIfBlank(
                                address.getCountry(),
                                DEFAULT_COUNTRY
                        )
                )
                .build();
    }

    private String defaultIfBlank(
            String value,
            String defaultValue
    ) {
        return StringUtils.hasText(value)
                ? value
                : defaultValue;
    }

    private BigDecimal resolveUnitPrice(SkuClientResponse sku) {
        return sku.getPrice() != null
                ? sku.getPrice()
                : BigDecimal.ZERO;
    }

    private String resolveProductName(SkuClientResponse sku) {
        return StringUtils.hasText(sku.getProductName())
                ? sku.getProductName()
                : "Product " + sku.getSkuCode();
    }


    private void createCancellationOutboxEvent(Order order) {
        String payload = serialize(
                new OrderStatusEvent(
                        order.getId(),
                        order.getStatus()
                                .name()
                )
        );

        OutboxEvent event = OutboxEvent.builder()
                .aggregateType(AGGREGATE_TYPE_ORDER)
                .aggregateId(order.getId())
                .eventType(EVENT_ORDER_CANCELLED)
                .payload(payload)
                .status(OutboxStatus.PENDING)
                .build();

        outboxEventRepository.save(event);
    }


    private void reserveStock(
            Order order,
            UUID customerId,
            List<ReservationItemClientRequest> reservationItems
    ) {
        inventoryServiceClient.reserveStock(
                StockReservationClientRequest.builder()
                        .orderId(order.getId())
                        .customerId(customerId)
                        .items(reservationItems)
                        .build()
        );
    }

    private String serialize(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            log.error("Failed to serialize outbox payload", ex);
            throw new IllegalStateException(
                    "Failed to serialize outbox event payload",
                    ex
            );
        }
    }

    private void releaseStock(UUID orderId) {
        inventoryServiceClient.releaseStock(
                StockReleaseClientRequest.builder()
                        .orderId(orderId)
                        .reason(CANCELLATION_REASON)
                        .build()
        );
    }

    private Order findOrder(UUID orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(
                        () -> new ResourceNotFoundException(
                                "Order not found with ID: " + orderId
                        )
                );
    }

    private void validateOrderAccess(
            Order order,
            UUID customerId,
            boolean isAdmin
    ) {
        if (!isAdmin && !order.getCustomerId()
                .equals(customerId)) {
            log.warn(
                    "Unauthorized access attempt: customerId={}, orderId={}",
                    customerId,
                    order.getId()
            );

            throw new InvalidOperationException(
                    "You are not authorized to view this order"
            );
        }
    }

    private void validateOrderOwnership(
            Order order,
            UUID customerId
    ) {
        if (!order.getCustomerId()
                .equals(customerId)) {
            throw new InvalidOperationException(
                    "You are not authorized to cancel this order"
            );
        }
    }

    private void validateCancellationState(Order order) {
        OrderStatus status = order.getStatus();

        if (status != OrderStatus.PENDING
                && status != OrderStatus.AWAITING_PAYMENT) {
            throw new InvalidStateTransitionException(
                    "Order cannot be cancelled in state: "
                            + status
                            + ". Only PENDING or AWAITING_PAYMENT orders can be cancelled."
            );
        }
    }

    private String generateOrderNumber() {
        String randomPart = UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, ORDER_NUMBER_LENGTH)
                .toUpperCase();

        return "ORD-"
                + Year.now(ZoneId.systemDefault())
                .getValue()
                + "-"
                + randomPart;
    }

    private record OrderBuildResult(
            Order order,
            List<ReservationItemClientRequest> reservationItems
    ) {
    }

    private record OrderStatusEvent(
            UUID orderId,
            String status
    ) {
    }
}

