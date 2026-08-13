package com.aegis.product_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class ProductRequest {

    @NotNull(message = "Category ID is required")
    private UUID categoryId;
    @NotBlank(message = "Title is required")
    private String title;
    @NotBlank(message = "Description is required")
    private String description;

    private List<SkuRequest> skus;

    private List<ProductAttributeRequest> productAttributes;
}
