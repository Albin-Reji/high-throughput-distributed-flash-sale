package com.project_aegis.inventory_service.dto.inventory.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryInitRequest {

    @NotNull
    private UUID skuId;

    @NotNull
    @Positive
    private Integer totalQuantity;
}
