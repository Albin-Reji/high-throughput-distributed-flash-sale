package com.aegis.product_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class SkuRequest {

    @NotBlank(message = "SKU code required")
    private String skuCode;

    private String color;

    private String size;

    @NotNull(message = "Price required")
    private BigDecimal price;
}