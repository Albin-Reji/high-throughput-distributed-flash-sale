package com.aegis.product_service.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class ProductSummaryResponse {

    private String title;
    private String description;
    private BigDecimal price;
}
