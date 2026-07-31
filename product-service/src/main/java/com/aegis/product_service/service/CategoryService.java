package com.aegis.product_service.service;

import com.aegis.product_service.dto.CategoryRequest;
import com.aegis.product_service.dto.CategoryResponse;
import com.aegis.product_service.dto.CategoryTreeResponse;
import com.aegis.product_service.dto.PageResponse;
import com.aegis.product_service.entity.Category;
import com.aegis.product_service.exception.ResourceNotFound;
import com.aegis.product_service.mapper.CategoryMapper;
import com.aegis.product_service.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
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
    /*
    * Retrieve all the categories from the categories DB
    * */
    public List<CategoryResponse> getAllCategories() {
        List<Category> categories = categoryRepository.findAll();
        log.info("categories size: {}", categories.size());
        return categories.stream()
                .map(categoryMapper::toResponse)
                .toList();
    }
    /*
     * Tree structure
     * */
    public List<CategoryTreeResponse> getCategoryTree() {
        List<Category> categories = categoryRepository.findAllByParentCategoryIsNull();

        return categories.stream()
                .map(categoryMapper::mapToTree)
                .toList();
    }
    /*
    * pagination based retrieval
    * */
    public PageResponse<CategoryResponse> getCategoryByPage(int page, int size) {
        Pageable pageable= PageRequest.of(
                page,
                size,
                Sort.by("name")
                );
        Page<CategoryResponse> pages=categoryRepository.findAll(pageable)
                .map(categoryMapper::toResponse);

        return PageResponse.<CategoryResponse>builder()
                 .content(pages.getContent())
                 .page(pages.getNumber())
                 .size(pages.getSize())
                 .totalElements(pages.getTotalElements())
                 .totalPages(pages.getTotalPages())
                 .first(pages.isFirst())
                 .last(pages.isLast())
                 .build();

    }
}
