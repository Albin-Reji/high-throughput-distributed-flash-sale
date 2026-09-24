package com.aegis.product_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkuResponse {

    private UUID id;
    private String skuCode;
    private String color;
    private String size;
    private BigDecimal price;
    private String productName;
}