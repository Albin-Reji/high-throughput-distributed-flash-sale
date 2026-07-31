package com.aegis.product_service.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class CategoryResponse {

    private UUID id;

    private String name;

    private UUID parentCategoryId;

    private String parentCategoryName;

    private LocalDateTime createdAt;
}
