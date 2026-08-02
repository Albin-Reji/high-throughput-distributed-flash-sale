package com.aegis.product_service.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class SkuResponse {

    private UUID id;
    private String skuCode;
    private String color;
    private String size;
    private BigDecimal price;
}