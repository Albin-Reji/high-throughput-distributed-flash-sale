package com.aegis.product_service.controller.admin;

import com.aegis.product_service.dto.common.PageResponse;
import com.aegis.product_service.dto.common.ProductSuccessResponse;
import com.aegis.product_service.dto.request.ProductRequest;
import com.aegis.product_service.dto.request.ProductUpdateRequest;
import com.aegis.product_service.dto.response.ProductResponse;
import com.aegis.product_service.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.repository.support.Repositories;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/products")
public class AdminProductController {

    private final ProductService productService;

    @PostMapping
    public ResponseEntity<ProductSuccessResponse> createProduct(@Valid @RequestBody ProductRequest request) {
        // Placeholder for product creation logic
        return ResponseEntity.ok(productService.createProduct(request));
    }
    @GetMapping
    public ResponseEntity<PageResponse<ProductResponse>> getAllProducts(@RequestParam(name="page", defaultValue = "0") int page,
                                                                        @RequestParam(name = "size", defaultValue = "10") int size) {
        return ResponseEntity.ok(productService.getAllProducts(page, size));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductSuccessResponse> updateProduct(@PathVariable("id") UUID id, @Valid @RequestBody ProductUpdateRequest request) {
        // Placeholder for product update logic
        return ResponseEntity.ok(productService.updateProduct(id, request));
    }
}
