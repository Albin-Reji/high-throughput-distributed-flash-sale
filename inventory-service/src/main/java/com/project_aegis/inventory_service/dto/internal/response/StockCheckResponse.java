package com.project_aegis.inventory_service.dto.internal.response;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockCheckResponse {

    private UUID skuId;

    private Integer totalQuantity;

    private Integer availableQuantity;

    private Integer reservedQuantity;

    private Boolean inStock;
}
