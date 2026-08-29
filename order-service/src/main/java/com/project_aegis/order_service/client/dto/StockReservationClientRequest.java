package com.project_aegis.order_service.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockReservationClientRequest {

    private UUID orderId;
    private UUID customerId;
    private UUID campaignId;
    private List<ReservationItemClientRequest> items;
    @Builder.Default
    private Integer timeoutSeconds = 900;
}
