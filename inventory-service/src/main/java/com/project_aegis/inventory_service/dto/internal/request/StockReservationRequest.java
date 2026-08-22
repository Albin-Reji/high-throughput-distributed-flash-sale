package com.project_aegis.inventory_service.dto.internal.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockReservationRequest {

    @NotNull
    private UUID orderId;

    private UUID customerId;

    private UUID campaignId;

    @NotEmpty
    @Valid
    private List<ReservationItemRequest> items;

    /**
     * Reservation timeout in seconds. Defaults to 900 (15 minutes).
     */
    @Builder.Default
    private Integer timeoutSeconds = 900;
}
