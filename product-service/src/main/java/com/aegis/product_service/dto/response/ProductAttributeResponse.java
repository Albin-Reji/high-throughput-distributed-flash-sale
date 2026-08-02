package com.aegis.product_service.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProductAttributeResponse {

    private String name;
    private String value;
}