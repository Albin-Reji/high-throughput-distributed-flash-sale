package com.aegis.product_service.controller.admin;

import com.aegis.product_service.dto.request.CategoryRequest;
import com.aegis.product_service.dto.response.CategoryResponse;
import com.aegis.product_service.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/categories")
public class AdminCategoryController {

    private final CategoryService categoryService;

    //    update the category
    @PutMapping("/{id}")
    public ResponseEntity<CategoryResponse> updateCategoryById(@PathVariable("id") UUID id, @Valid @RequestBody CategoryRequest request) {
        return ResponseEntity.ok(categoryService.putCategory(id, request));
    }

    //   partial update the category
    @PatchMapping("/{id}")
    public ResponseEntity<CategoryResponse> updateCategory(@PathVariable("id") UUID id, @Valid @RequestBody CategoryRequest request) {
        return ResponseEntity.ok(categoryService.updateCategory(id, request));
    }

    //delete categories
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable("id") UUID id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.noContent()
                .build();
    }
}
