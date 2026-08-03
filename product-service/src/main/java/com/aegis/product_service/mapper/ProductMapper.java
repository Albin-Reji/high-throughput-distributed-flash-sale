package com.aegis.product_service.mapper;

import com.aegis.product_service.dto.request.ProductAttributeRequest;
import com.aegis.product_service.dto.request.SkuRequest;
import com.aegis.product_service.dto.response.*;
import com.aegis.product_service.entity.Product;
import com.aegis.product_service.entity.ProductAttribute;
import com.aegis.product_service.entity.Sku;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class ProductMapper {

    public ProductResponse toResponse(Product product) {

        return ProductResponse.builder()
                .id(product.getId())
                .title(product.getTitle())
                .description(product.getDescription())

                .category(
                        ProductCategoryResponse.builder()
                                .id(product.getCategory().getId())
                                .name(product.getCategory().getName())
                                .build()
                )

                .skus(
                        product.getSkus()
                                .stream()
                                .map(sku -> SkuResponse.builder()
                                        .id(sku.getId())
                                        .skuCode(sku.getSkuCode())
                                        .color(sku.getColor())
                                        .size(sku.getSize())
                                        .price(sku.getPrice())
                                        .build()
                                )
                                .toList()
                )

                .productAttributes(
                        product.getProductAttributes()
                                .stream()
                                .map(attr -> ProductAttributeResponse.builder()
                                        .name(attr.getAttributeName())
                                        .value(attr.getAttributeValue())
                                        .build()
                                )
                                .toList()
                )

                .build();
    }

    public ProductAttribute toEntity(ProductAttributeRequest productAttributeRequest, Product product) {
        return ProductAttribute.builder()
                .attributeName(productAttributeRequest.getName())
                .attributeValue(productAttributeRequest.getValue())
                .product(product)
                .build();
    }

    public Sku toEntity(SkuRequest skuRequest, Product product) {
        return Sku.builder()
                .skuCode(skuRequest.getSkuCode())
                .size(skuRequest.getSize())
                .color(skuRequest.getColor())
                .price(skuRequest.getPrice())
                .product(product)
                .build();
    }

    public ProductSummaryResponse toSummaryResponse(Product product) {
        return ProductSummaryResponse.builder()
                .title(product.getTitle())
                .description(product.getDescription())
                .price(product.getSkus()
                        .stream()
                        .map(Sku::getPrice)
                        .min(BigDecimal::compareTo)
                        .orElse(null))
                .build();
    }

}
