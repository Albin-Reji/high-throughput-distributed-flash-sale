package com.aegis.product_service.controller.publicapi;

import com.aegis.product_service.dto.common.PageResponse;
import com.aegis.product_service.dto.response.ProductAttributeResponse;
import com.aegis.product_service.dto.response.ProductResponse;
import com.aegis.product_service.dto.response.ProductSummaryResponse;
import com.aegis.product_service.dto.response.SkuResponse;
import com.aegis.product_service.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
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

    @GetMapping("/category/{categoryId}")
    public ResponseEntity<PageResponse<ProductSummaryResponse>> getProductsByCategory(
            @PageableDefault(
                    page = 0,
                    size = 10,
                    sort = "title",
                    direction = Sort.Direction.ASC
            ) Pageable pageable,
            @PathVariable UUID categoryId
    ) {
        return ResponseEntity.ok(productService.getProductsByCategory(pageable, categoryId));
    }

    @GetMapping("{productId}/skus")
    public ResponseEntity<PageResponse<SkuResponse>> getAllSkusByProductId(
            @PageableDefault(
                    page = 0,
                    size = 10,
                    sort = "skuCode",
                    direction = Sort.Direction.ASC
            ) Pageable pageable,
            @PathVariable UUID productId
    ) {
        return ResponseEntity.ok(productService.getAllSkusByProductId(pageable, productId));
    }
    @GetMapping("/{productId}/attributes")
    public ResponseEntity<PageResponse<ProductAttributeResponse>> getProductAttributesByProductId(
            @PageableDefault(
                    page = 0,
                    size = 10,
                    sort = "attributeName",
                    direction = Sort.Direction.ASC
            ) Pageable pageable,
            @PathVariable UUID productId
    ) {
        return ResponseEntity.ok(productService.getProductAttributesByProductId(pageable, productId));
    }

    @GetMapping("/skus/{skuId}")
    public SkuResponse isSkuExist(@PathVariable("skuId") UUID skuId){
        log.info("The /skus/{skuId} called In PRODUCT-SERVICE with Id: {}", skuId);
        return productService.isSkuExist(skuId);
    }
}
