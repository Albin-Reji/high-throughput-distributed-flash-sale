package com.project_aegis.inventory_service.dto.inventory.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryAdjustRequest {

    @NotNull
    private Integer quantityDelta;

    @NotBlank
    private String reason;
}
