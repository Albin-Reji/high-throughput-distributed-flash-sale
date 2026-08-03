package com.aegis.product_service.controller.publicapi;

import com.aegis.product_service.dto.common.PageResponse;
import com.aegis.product_service.dto.response.ProductResponse;
import com.aegis.product_service.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class PublicProductController {

    private final ProductService productService;

    @GetMapping
    public ResponseEntity<PageResponse<ProductResponse>> getAllProducts(
            @PageableDefault(
                    page = 0,
                    size = 10,
                    sort = "title",
                    direction = Sort.Direction.ASC
            ) Pageable pageable
    ) {
        return ResponseEntity.ok(productService.getAllProductsByPage(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProductById(@PathVariable UUID id) {
        return ResponseEntity.ok(productService.getProductById(id));
    }

    @GetMapping("/search")
    public ResponseEntity<PageResponse<ProductResponse>> searchProducts(
            @PageableDefault(
                    page = 0,
                    size = 10,
                    sort = "title",
                    direction = Sort.Direction.ASC
            ) Pageable pageable,
            @RequestParam("query") String query
    ) {
        return ResponseEntity.ok(productService.searchProductsByTitle(pageable, query));
    }
}
