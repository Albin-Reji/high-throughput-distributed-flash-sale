package com.aegis.product_service.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
public class CategoryTreeResponse {
    private UUID id;
    private String name;
    private List<CategoryTreeResponse> children;
}
