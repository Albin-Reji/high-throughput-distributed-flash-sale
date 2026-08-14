package com.aegis.product_service.controller.admin;

import com.aegis.product_service.dto.common.PageResponse;
import com.aegis.product_service.dto.common.ProductAttributeSuccessResponse;
import com.aegis.product_service.dto.common.ProductSuccessResponse;
import com.aegis.product_service.dto.common.SkuSuccessResponse;
import com.aegis.product_service.dto.request.*;
import com.aegis.product_service.dto.response.ProductAttributeResponse;
import com.aegis.product_service.dto.response.ProductResponse;
import com.aegis.product_service.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
    public ResponseEntity<PageResponse<ProductResponse>> getAllProducts(@RequestParam(name = "page", defaultValue = "0") int page,
                                                                        @RequestParam(name = "size", defaultValue = "10") int size) {
        return ResponseEntity.ok(productService.getAllProducts(page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProductById(@PathVariable UUID id) {
        return ResponseEntity.ok(productService.getProductById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductSuccessResponse> updateProduct(@PathVariable("id") UUID id, @Valid @RequestBody ProductUpdateRequest request) {
        return ResponseEntity.ok(productService.updateProduct(id, request));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ProductSuccessResponse> patchProduct(@PathVariable("id") UUID id, @Valid @RequestBody ProductPatchRequest request) {
        return ResponseEntity.ok(productService.patchProduct(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ProductSuccessResponse> deleteProduct(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(productService.deleteProduct(id));
    }

    @PostMapping("/{productId}/skus")
    public ResponseEntity<SkuSuccessResponse> createSku(@PathVariable("productId") UUID productId, @Valid @RequestBody SkuRequest request) {
        return ResponseEntity.ok(productService.createSku(productId, request));
    }

    @PutMapping("/{productId}/skus/{skuId}")
    public ResponseEntity<SkuSuccessResponse> updateSku(@PathVariable("productId") UUID productId, @PathVariable("skuId") UUID skuId, @Valid @RequestBody SkuUpdateRequest request) {
        return ResponseEntity.ok(productService.updateSku(productId, skuId, request));
    }

    @DeleteMapping("{productId}/skus/{skuId}")
    public ResponseEntity<SkuSuccessResponse> deleteSkuBySkuId(
            @PathVariable("productId") UUID productId, @PathVariable("skuId") UUID skuId
    ) {
        return ResponseEntity.ok(productService.deleteSkuBySkuId(productId, skuId));
    }

    @PostMapping("/{productId}/attributes")
    public ResponseEntity<ProductAttributeSuccessResponse> createProductAttribute(@PathVariable("productId") UUID productId, @Valid @RequestBody ProductAttributeRequest request) {
        return ResponseEntity.ok(productService.createProductAttribute(productId, request));
    }
    @PatchMapping("/{productId}/attributes/{attributeName}")
    public ResponseEntity<ProductAttributeSuccessResponse> updateProductAttribute(@PathVariable("productId") UUID productId, @PathVariable("attributeName") String attributeName,  @Valid @RequestBody ProductAttributeUpdateRequest request) {
        return ResponseEntity.ok(productService.updateProductAttribute(productId, attributeName, request));
    }
    @DeleteMapping("/{productId}/attributes/{attributeName}")
    public ResponseEntity<ProductAttributeSuccessResponse> deleteProductAttribute(@PathVariable("productId") UUID productId, @PathVariable("attributeName") String attributeName) {
        return ResponseEntity.ok(productService.deleteProductAttribute(productId, attributeName));
    }
}
