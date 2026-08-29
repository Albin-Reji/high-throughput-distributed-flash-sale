package com.project_aegis.order_service.dto.response;

import com.project_aegis.order_service.entity.OrderStatus;
import com.project_aegis.order_service.entity.OrderType;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderDetailResponse {

    private UUID orderId;
    private String orderNumber;
    private UUID customerId;
    private OrderStatus status;
    private OrderType orderType;
    private BigDecimal subtotalAmount;
    private BigDecimal taxAmount;
    private BigDecimal shippingFee;
    private BigDecimal totalAmount;
    private String currency;
    private String trackingNumber;
    private String carrier;
    private Instant createdAt;
    private Instant updatedAt;
    private OrderShippingAddressResponse shippingAddress;
    private List<OrderItemResponse> items;
}
