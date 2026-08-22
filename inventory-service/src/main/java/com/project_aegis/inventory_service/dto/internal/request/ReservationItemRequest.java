package com.project_aegis.inventory_service.dto.internal.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReservationItemRequest {

    @NotNull
    private UUID skuId;

    @NotNull
    @Positive
    private Integer quantity;
}
