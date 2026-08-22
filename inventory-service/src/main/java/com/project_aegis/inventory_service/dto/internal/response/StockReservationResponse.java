package com.project_aegis.inventory_service.dto.internal.response;

import lombok.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockReservationResponse {

    private UUID orderId;

    private UUID customerId;

    private UUID campaignId;

    private String status;

    private Instant expiresAt;

    private List<ReservedItemResponse> items;
}
