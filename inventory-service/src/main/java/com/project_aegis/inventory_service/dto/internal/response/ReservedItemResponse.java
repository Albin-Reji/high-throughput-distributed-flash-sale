package com.project_aegis.inventory_service.dto.internal.response;

import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReservedItemResponse {

    private UUID skuId;

    private Integer quantity;

    private BigDecimal flashPrice;
}
