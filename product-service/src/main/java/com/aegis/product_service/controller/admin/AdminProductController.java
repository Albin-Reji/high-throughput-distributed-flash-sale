package com.aegis.product_service.controller.admin;

import com.aegis.product_service.dto.request.ProductRequest;
import com.aegis.product_service.dto.response.ProductResponse;
import com.aegis.product_service.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/products")
public class AdminProductController {

    private final ProductService productService;

    @PostMapping
    public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody ProductRequest request) {
        // Placeholder for product creation logic
        return ResponseEntity.ok(productService.createProduct(request));
    }
}
