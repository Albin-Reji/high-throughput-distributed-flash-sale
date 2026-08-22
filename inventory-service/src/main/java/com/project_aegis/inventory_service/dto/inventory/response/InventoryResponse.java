package com.project_aegis.inventory_service.dto.inventory.response;

import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryResponse {

    private UUID skuId;

    private Integer totalQuantity;

    private Integer availableQuantity;

    private Integer reservedQuantity;

    private Instant updatedAt;
}
