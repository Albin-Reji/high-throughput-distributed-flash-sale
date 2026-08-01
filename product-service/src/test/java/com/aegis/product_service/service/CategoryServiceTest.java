package com.aegis.product_service.service;

import com.aegis.product_service.dto.CategoryRequest;
import com.aegis.product_service.dto.CategoryResponse;
import com.aegis.product_service.entity.Category;
import com.aegis.product_service.exception.ResourceNotFound;
import com.aegis.product_service.mapper.CategoryMapper;
import com.aegis.product_service.repository.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Spy
    private CategoryMapper categoryMapper;

    @InjectMocks
    private CategoryService categoryService;

    private CategoryRequest request;

    @BeforeEach
    void setUp() {
        request = new CategoryRequest();
        request.setName("Electronics");
        request.setParentCategoryId(null);
    }

    @Test
    void createCategory_withNullParentCategoryId_success() {
        Category savedCategory = Category.builder()
                .id(UUID.randomUUID())
                .name("Electronics")
                .parentCategory(null)
                .build();

        given(categoryRepository.save(any(Category.class))).willReturn(savedCategory);

        CategoryResponse response = categoryService.createCategory(request);

        assertThat(response).isNotNull();
        assertThat(response.getName()).isEqualTo("Electronics");
        assertThat(response.getParentCategoryId()).isNull();
        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    void createCategory_withNonExistentParentCategoryId_throwsResourceNotFound() {
        UUID parentId = UUID.randomUUID();
        request.setParentCategoryId(parentId);

        given(categoryRepository.findById(parentId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.createCategory(request))
                .isInstanceOf(ResourceNotFound.class)
                .hasMessageContaining("Parent category not found with id: " + parentId);
    }
}
