package com.project_aegis.order_service.mapper;

import com.project_aegis.order_service.dto.response.*;
import com.project_aegis.order_service.entity.Order;
import com.project_aegis.order_service.entity.OrderItem;
import com.project_aegis.order_service.entity.OrderShippingAddress;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class OrderMapper {

    public OrderSummaryResponse toSummaryResponse(Order order) {
        if (order == null) return null;
        return OrderSummaryResponse.builder()
                .orderId(order.getId())
                .orderNumber(order.getOrderNumber())
                .status(order.getStatus())
                .totalAmount(order.getTotalAmount())
                .createdAt(order.getCreatedAt())
                .build();
    }

    public CreateOrderResponse toCreateResponse(Order order) {
        if (order == null) return null;
        return CreateOrderResponse.builder()
                .orderId(order.getId())
                .orderNumber(order.getOrderNumber())
                .status(order.getStatus())
                .totalAmount(order.getTotalAmount())
                .currency(order.getCurrency())
                .build();
    }

    public OrderDetailResponse toDetailResponse(Order order) {
        if (order == null) return null;
        return OrderDetailResponse.builder()
                .orderId(order.getId())
                .orderNumber(order.getOrderNumber())
                .customerId(order.getCustomerId())
                .status(order.getStatus())
                .orderType(order.getOrderType())
                .subtotalAmount(order.getSubtotalAmount())
                .taxAmount(order.getTaxAmount())
                .shippingFee(order.getShippingFee())
                .totalAmount(order.getTotalAmount())
                .currency(order.getCurrency())
                .trackingNumber(order.getTrackingNumber())
                .carrier(order.getCarrier())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .shippingAddress(toShippingAddressResponse(order.getShippingAddress()))
                .items(toItemResponseList(order.getItems()))
                .build();
    }

    public OrderItemResponse toItemResponse(OrderItem item) {
        if (item == null) return null;
        return OrderItemResponse.builder()
                .id(item.getId())
                .skuId(item.getSkuId())
                .skuCode(item.getSkuCode())
                .productName(item.getProductName())
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .subtotal(item.getSubtotal())
                .build();
    }

    public List<OrderItemResponse> toItemResponseList(List<OrderItem> items) {
        if (items == null) return Collections.emptyList();
        return items.stream()
                .map(this::toItemResponse)
                .toList();
    }

    public OrderShippingAddressResponse toShippingAddressResponse(OrderShippingAddress address) {
        if (address == null) return null;
        return OrderShippingAddressResponse.builder()
                .recipientName(address.getRecipientName())
                .addressLine1(address.getAddressLine1())
                .addressLine2(address.getAddressLine2())
                .city(address.getCity())
                .state(address.getState())
                .postalCode(address.getPostalCode())
                .country(address.getCountry())
                .build();
    }
}
