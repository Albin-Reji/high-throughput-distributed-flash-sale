package com.project_aegis.order_service.dto.response;

import com.project_aegis.order_service.entity.OrderStatus;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderResponse {

    private UUID orderId;
    private String orderNumber;
    private OrderStatus status;
    private BigDecimal totalAmount;
    private String currency;
}
