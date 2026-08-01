package com.aegis.product_service.mapper;

import com.aegis.product_service.dto.CategoryResponse;
import com.aegis.product_service.dto.CategoryTreeResponse;
import com.aegis.product_service.entity.Category;
import org.springframework.stereotype.Component;


@Component
public class CategoryMapper {


    public CategoryResponse toResponse(Category category) {

        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .createdAt(category.getCreatedAt())
                .parentCategoryId(
                        category.getParentCategory() != null ?
                                category.getParentCategory().getId() : null
                )
                .parentCategoryName(
                        category.getParentCategory() != null ?
                                category.getParentCategory().getName() : null
                )
                .build();
    }

    public CategoryTreeResponse mapToTree(Category category) {
        CategoryTreeResponse response = new CategoryTreeResponse();


        response.setId(category.getId());
        response.setName(category.getName());


        response.setChildren(
                category.getChildren()
                        .stream()
                        .map(this::mapToTree)
                        .toList()
        );


        return response;
    }
}
