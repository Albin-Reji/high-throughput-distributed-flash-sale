package com.aegis.product_service.service;

import com.aegis.product_service.dto.CategoryRequest;
import com.aegis.product_service.dto.CategoryResponse;
import com.aegis.product_service.entity.Category;
import com.aegis.product_service.exception.ResourceNotFound;
import com.aegis.product_service.mapper.CategoryMapper;
import com.aegis.product_service.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    public CategoryResponse createCategory(CategoryRequest request) {

        Category parentCategory = null;

        if(request.getParentCategoryId() != null){
            parentCategory = categoryRepository.findById(request.getParentCategoryId())
                    .orElseThrow(
                            () -> new ResourceNotFound("Parent category not found with id: "
                                    + request.getParentCategoryId()));
        }

        // Logic to create a new category and save it to the repository
        Category newCategory = Category.builder()
                .name(request.getName())
                .parentCategory(parentCategory)
                .build();

        Category savedCategory = categoryRepository.save(newCategory);
        // map to CategoryResponse Dto and return
        return categoryMapper.toResponse(savedCategory);

    }
}
