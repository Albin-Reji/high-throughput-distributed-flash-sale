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
import com.fasterxml.jackson.databind.ObjectMapper;

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

/**
 * Coordinates the order creation workflow using synchronous Saga-style
 * orchestration.
 *
 * <p>The workflow consists of the following steps:</p>
 *
 * <ol>
 *     <li>
 *         Checks the idempotency key and returns the previously cached
 *         response if the request has already been processed.
 *     </li>
 *     <li>
 *         Retrieves and validates the customer's shipping address from
 *         the User Service.
 *     </li>
 *     <li>
 *         Builds and persists the order with {@link OrderStatus#PENDING} status.
 *     </li>
 *     <li>
 *         Reserves the required inventory through the Inventory Service.
 *     </li>
 *     <li>
 *         If inventory reservation succeeds, transitions the order to
 *         {@link OrderStatus#AWAITING_PAYMENT} and persists the corresponding
 *         outbox event and idempotency record atomically.
 *     </li>
 *     <li>
 *         If a failure occurs after inventory has been reserved, executes
 *         the compensating transaction by releasing the reserved inventory.
 *     </li>
 *     <li>
 *         Marks the order as {@link OrderStatus#FAILED} when the workflow
 *         cannot be completed.
 *     </li>
 * </ol>
 *
 * <p>
 * <strong>Important:</strong>
 * This is a synchronous Saga-style orchestration. The Order Service
 * coordinates operations across service boundaries, so a database
 * transaction cannot roll back changes already committed by the
 * Inventory Service. Explicit compensating transactions are therefore
 * required to maintain business consistency.
 * </p>
 *
 * @param customerId    the unique identifier of the customer creating the order
 * @param idempotencyKey the idempotency key used to safely handle duplicate requests
 * @param request       the order creation request containing items and shipping details
 * @param bearerToken   the customer's bearer token used for downstream service calls
 *
 * @return the created order response when the order workflow completes successfully
 *
 * @throws InvalidOperationException if inventory reservation, order confirmation,
 *                                   or compensation-related workflow processing fails
 */

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

        boolean stockReserved = false;
        // If stock reservation or order confirmation fails,
        // OrderStatus={@link OrderStatus#FAILED}, compensate stock if reserved, and return error response to client
        try {
            reserveStock(
                    savedOrder,
                    customerId,
                    orderBuildResult.reservationItems()
            );
            stockReserved = true;

            // if reserveStock() is success then confirm the order
            Order confirmOrder = orderPersistenceService.confirmOrder(
                    savedOrder,
                    customerId,
                    idempotencyKey
            );

            return orderMapper.toCreateResponse(confirmOrder);

        } catch (Exception e) {
            log.error(
                    "Failed to complete order creation for orderId: {}. Initiating rollback/compensation.",
                    savedOrder.getId(),
                    e
            );

            if (stockReserved) {
                // if stock is reserved then release the stock
                try {
                    releaseStock(savedOrder.getId());

                } catch (Exception ex) {
                    log.error(
                            "Failed to release stock for orderId: {}. Manual intervention required.",
                            savedOrder.getId(),
                            ex
                    );
                }

            }
            try {
                orderPersistenceService.failOrder(savedOrder);

            } catch (Exception ex) {
                log.error(
                        "CRITICAL: Failed to mark order as FAILED for orderId: {}. Manual intervention required.",
                        savedOrder.getId(),
                        ex
                );
            }

            if (e instanceof InvalidOperationException invalidOperationException) {
                throw invalidOperationException;
            }
            String errorMessage = stockReserved
                    ? "Order confirmation failed. Stock reservation compensated and order marked as FAILED."
                    : "Stock reservation failed. Order has been marked as FAILED.";
            throw new InvalidOperationException(errorMessage, e);

        }

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

