package com.aegis.product_service.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.UUID;

@Data
public class CategoryRequest {

    @NotBlank(message = "Category name is required")
    private String name;

    private UUID parentCategoryId;

}
