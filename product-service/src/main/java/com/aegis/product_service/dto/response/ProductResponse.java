package com.aegis.product_service.dto.response;

import com.aegis.product_service.entity.Category;
import com.aegis.product_service.entity.ProductAttribute;
import com.aegis.product_service.entity.Sku;
import lombok.Builder;
import lombok.Data;


import java.util.List;
import java.util.UUID;


@Data
@Builder
public class ProductResponse {

    private UUID id;
    private String title;
    private String description;

    private CategoryResponse category;

    private List<SkuResponse> skus;

    private List<ProductAttributeResponse> productAttributes;
}