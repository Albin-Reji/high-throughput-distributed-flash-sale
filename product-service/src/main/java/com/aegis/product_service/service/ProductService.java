package com.aegis.product_service.service;

import com.aegis.product_service.dto.request.ProductRequest;
import com.aegis.product_service.dto.response.ProductResponse;
import com.aegis.product_service.entity.Category;
import com.aegis.product_service.entity.Product;
import com.aegis.product_service.entity.ProductAttribute;
import com.aegis.product_service.entity.Sku;
import com.aegis.product_service.exception.ResourceAlreadyExists;
import com.aegis.product_service.exception.ResourceNotFound;
import com.aegis.product_service.mapper.ProductMapper;
import com.aegis.product_service.repository.CategoryRepository;
import com.aegis.product_service.repository.ProductRepository;
import com.aegis.product_service.repository.SkuRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final SkuRepository skuRepository;
    private final ProductMapper productMapper;

    @Transactional
    public ProductResponse createProduct(@Valid ProductRequest request) {
// check if category already exist
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFound("Category not found with id: " + request.getCategoryId()));
// check for skucode is already exist
        request.getSkus().forEach(skuRequest -> {
            if (skuRepository.existsBySkuCode(skuRequest.getSkuCode())) {
                throw new ResourceAlreadyExists("SKU code already exists: " + skuRequest.getSkuCode());
            }
        });

        Product product = Product.builder().category(category).title(request.getTitle()).description(request.getDescription()).build();

        List<ProductAttribute> productAttributes = request.getProductAttributes().stream().map(prodAttr -> productMapper.toEntity(prodAttr, product)).collect(Collectors.toCollection(ArrayList::new));

        List<Sku> skus = request.getSkus().stream().map(skuRequest -> productMapper.toEntity(skuRequest, product)).collect(Collectors.toCollection(ArrayList::new));

        product.setProductAttributes(productAttributes);
        product.setSkus(skus);

        Product savedProduct = productRepository.save(product);
        return productMapper.toResponse(savedProduct);
    }
}
