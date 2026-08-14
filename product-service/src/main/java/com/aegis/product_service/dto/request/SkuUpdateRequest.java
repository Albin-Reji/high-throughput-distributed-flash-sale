package com.aegis.product_service.dto.request;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SkuUpdateRequest {

    private String color;

    private String size;

    private BigDecimal price;

}
