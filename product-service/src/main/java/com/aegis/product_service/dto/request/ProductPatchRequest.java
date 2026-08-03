package com.aegis.product_service.dto.request;

import lombok.Data;

import java.util.UUID;

@Data
public class ProductPatchRequest {

    private String title;

    private String description;

    private UUID categoryId;
}
