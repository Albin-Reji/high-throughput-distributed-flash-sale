package com.project_aegis.inventory_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SkuResponse {

    private UUID id;
    private String skuCode;
    private String color;
    private String size;
    private BigDecimal price;
}