package com.aegis.product_service.dto.common;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProductAttributeSuccessResponse {

    private String name;
    private String value;
    private String message;

}
