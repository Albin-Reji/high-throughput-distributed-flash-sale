package com.aegis.product_service.controller;

import com.aegis.product_service.dto.CategoryRequest;
import com.aegis.product_service.dto.CategoryResponse;
import com.aegis.product_service.dto.CategoryTreeResponse;
import com.aegis.product_service.dto.PageResponse;
import com.aegis.product_service.service.CategoryService;
import jakarta.validation.Valid;
import jakarta.websocket.server.PathParam;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/categories")
public class CategoryRestController {

    private final CategoryService categoryService;

    @PostMapping
    public ResponseEntity<CategoryResponse> createCategory(@Valid @RequestBody CategoryRequest request){
        return ResponseEntity.ok(categoryService.createCategory(request));
    }

    @GetMapping("/all")
    public ResponseEntity<List<CategoryResponse>> getAllCategories(){
        return ResponseEntity.ok(categoryService.getAllCategories());
    }
    @GetMapping("/tree")
    public ResponseEntity<List<CategoryTreeResponse>> getCategoryTree(){
        return ResponseEntity.ok(categoryService.getCategoryTree());
    }
    @GetMapping
    public ResponseEntity<PageResponse<CategoryResponse>> getCategoryByPage(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size
    ){
        return ResponseEntity.ok(categoryService.getCategoryByPage(page, size));
    }
}
