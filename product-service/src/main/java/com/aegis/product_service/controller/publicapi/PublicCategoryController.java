package com.aegis.product_service.controller.publicapi;

import com.aegis.product_service.dto.request.CategoryRequest;
import com.aegis.product_service.dto.response.CategoryResponse;
import com.aegis.product_service.dto.response.CategoryTreeResponse;
import com.aegis.product_service.dto.common.PageResponse;
import com.aegis.product_service.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/categories")
public class PublicCategoryController {

    private final CategoryService categoryService;

    // api to create new category
    @PostMapping
    public ResponseEntity<CategoryResponse> createCategory(@Valid @RequestBody CategoryRequest request) {
        return ResponseEntity.ok(categoryService.createCategory(request));
    }

    //  api to get-all categories
    @GetMapping("/all")
    public ResponseEntity<List<CategoryResponse>> getAllCategories() {
        return ResponseEntity.ok(categoryService.getAllCategories());
    }

    //  api to get-tree structure for categories
    @GetMapping("/tree")
    public ResponseEntity<List<CategoryTreeResponse>> getCategoryTree() {
        return ResponseEntity.ok(categoryService.getCategoryTree());
    }

    //  api to get categories by page
    @GetMapping
    public ResponseEntity<PageResponse<CategoryResponse>> getCategoryByPage(@RequestParam(name = "page", defaultValue = "0") int page, @RequestParam(name = "size", defaultValue = "10") int size) {
        return ResponseEntity.ok(categoryService.getCategoryByPage(page, size));
    }

    //    get the category by id
    @GetMapping("/{id}")
    public ResponseEntity<CategoryResponse> getCategoryById(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(categoryService.getCategoryById(id));
    }

    //    search categories
    @GetMapping("/search")
    public ResponseEntity<List<CategoryResponse>> searchCategory(@RequestParam("name") String name) {
        return ResponseEntity.ok(categoryService.searchCategories(name));
    }


}
