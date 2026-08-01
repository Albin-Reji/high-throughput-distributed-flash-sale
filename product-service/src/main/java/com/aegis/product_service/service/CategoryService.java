package com.aegis.product_service.service;

import com.aegis.product_service.dto.request.CategoryRequest;
import com.aegis.product_service.dto.response.CategoryResponse;
import com.aegis.product_service.dto.response.CategoryTreeResponse;
import com.aegis.product_service.dto.common.PageResponse;
import com.aegis.product_service.entity.Category;
import com.aegis.product_service.exception.ResourceAlreadyExists;
import com.aegis.product_service.exception.ResourceNotFound;
import com.aegis.product_service.mapper.CategoryMapper;
import com.aegis.product_service.repository.CategoryRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    //    create new category

    public CategoryResponse createCategory(CategoryRequest request) {

        Category parentCategory = null;

        if (request.getParentCategoryId() != null) {
            parentCategory = categoryRepository.findById(request.getParentCategoryId())
                    .orElseThrow(
                            () -> new ResourceNotFound("Parent category not found with id: "
                                    + request.getParentCategoryId()));
        }
        if (categoryRepository.existsByName(request.getName())) {
            throw new ResourceAlreadyExists("Category with name '" + request.getName() + "' already exists.");
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

    /**
     * Retrieve all the categories from the categories DB
     *
     */
    public List<CategoryResponse> getAllCategories() {
        List<Category> categories = categoryRepository.findAll();
        log.info("categories size: {}", categories.size());
        return categories.stream()
                .map(categoryMapper::toResponse)
                .toList();
    }

    /**
     * Returns only root categories with nested children.
     * <p>
     * This avoids returning a flat category list because
     * the frontend category menu requires hierarchical data.
     */
    public List<CategoryTreeResponse> getCategoryTree() {
        List<Category> categories = categoryRepository.findAllByParentCategoryIsNull();

        return categories.stream()
                .map(categoryMapper::mapToTree)
                .toList();
    }

    /**
     * pagination based retrieval
     */
    public PageResponse<CategoryResponse> getCategoryByPage(int page, int size) {
        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by("name")
        );
        Page<CategoryResponse> pages = categoryRepository.findAll(pageable)
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

    //    get category by Id
    public CategoryResponse getCategoryById(UUID id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFound("Category not found with id: " + id));
        return categoryMapper.toResponse(category);
    }

    public CategoryResponse updateCategory(UUID id, @Valid CategoryRequest request) {
        if (categoryRepository.existsById(id)) {
            Category category = categoryRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFound("Category not found with id: " + id));
            category.setName(request.getName() != null ? request.getName() : category.getName());


            if (request.getParentCategoryId() != null) {
                Category finalParentCategory = categoryRepository.findById(request.getParentCategoryId())
                        .orElseThrow(() -> new ResourceNotFound("Parent category not found with id: " + request.getParentCategoryId()));

                category.setParentCategory(finalParentCategory);
            }
            categoryRepository.save(category);
            return categoryMapper.toResponse(category);
        }

        throw new ResourceNotFound("Category not found with id: " + id);


    }

    //delete the category based on Id
    public void deleteCategory(UUID id) {

        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFound("Category not found with id: " + id));

        categoryRepository.delete(category);
    }

    //update the category
    public CategoryResponse putCategory(UUID id, @Valid CategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFound("Category not found with id: " + id));
        Category parentCategory = categoryRepository.findById(request.getParentCategoryId())
                .orElseThrow(() -> new ResourceNotFound("Parent category not found with id: " + request.getParentCategoryId()));

        category.setName(request.getName() != null ? request.getName() : category.getName());
        category.setParentCategory(parentCategory);

        categoryRepository.save(category);
        return categoryMapper.toResponse(category);
    }

    //search by name
    public List<CategoryResponse> searchCategories(String name) {
        if (name == null ||  name.isEmpty()) {
            throw new ResourceNotFound("Category not found with name: " + name);
        }
        return categoryRepository.findAllByNameContainingIgnoreCase(name)
                .stream()
                .map(categoryMapper::toResponse)
                .toList();
    }
}