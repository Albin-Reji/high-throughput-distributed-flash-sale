package com.aegis.product_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ProductAttributeRequest {

    @NotBlank(message = "Attribute name required")
    private String name;

    @NotBlank(message = "Attribute value required")
    private String value;
}