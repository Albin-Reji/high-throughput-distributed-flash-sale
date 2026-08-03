package com.aegis.product_service.service;

import com.aegis.product_service.dto.common.PageResponse;
import com.aegis.product_service.dto.common.ProductSuccessResponse;
import com.aegis.product_service.dto.request.ProductPatchRequest;
import com.aegis.product_service.dto.request.ProductRequest;
import com.aegis.product_service.dto.request.ProductUpdateRequest;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final SkuRepository skuRepository;
    private final ProductMapper productMapper;


    /**
     * <p>Creates a new product.</p>
     *
     * @param request
     * @return {@link ProductSuccessResponse}
     */
    @Transactional
    public ProductSuccessResponse createProduct(@Valid ProductRequest request) {
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
        return new ProductSuccessResponse(savedProduct.getId(), savedProduct.getTitle(), "product created successfully");
    }

    /**
     * <p>Updates a product by its ID.</p>
     *
     * @param id
     * @param request
     * @return {@link ProductSuccessResponse}
     */
    @Transactional
    public ProductSuccessResponse updateProduct(UUID id, @Valid ProductUpdateRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFound("Product not found with id: " + id));

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFound("Category not found with id: " + request.getCategoryId()));

        product.setCategory(category);
        product.setTitle(request.getTitle());
        product.setDescription(request.getDescription());

        productRepository.save(product);
        return new ProductSuccessResponse(
                product.getId(),
                product.getTitle(),
                "product updated successfully");
    }

    public PageResponse<ProductResponse> getAllProducts(int page, int size) {
        Pageable pageable = PageRequest.of(
                page,
                size);

        Page<ProductResponse> productPage = productRepository.findAll(pageable)
                .map(productMapper::toResponse);

        return PageResponse.<ProductResponse>builder()
                .content(productPage.getContent())
                .first(productPage.isFirst())
                .last(productPage.isLast())
                .size(productPage.getSize())
                .totalPages(productPage.getTotalPages())
                .totalElements(productPage.getTotalElements())
                .build();
    }

    public ProductResponse getProductById(UUID id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFound("Product not found with id: " + id));

        return productMapper.toResponse(product);
    }

    @Transactional
    public ProductSuccessResponse patchProduct(UUID id, @Valid ProductPatchRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFound("Product not found with id: " + id));

        Category requestCategory = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFound("Category not found with id: " + request.getCategoryId()));

        product.setTitle(request.getTitle() != null ? request.getTitle() : product.getTitle());
        product.setDescription(request.getDescription() != null ? request.getDescription() : product.getDescription());
        product.setCategory(requestCategory);

        productRepository.save(product);
        return new ProductSuccessResponse(
                product.getId(),
                product.getTitle(),
                "product updated successfully");
    }

    @Transactional
    public ProductSuccessResponse deleteProduct(UUID id) {
        Product product= productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFound("Product not found with id: " + id));

        productRepository.delete(product);

        return new ProductSuccessResponse(
                product.getId(),
                product.getTitle(),
                "product deleted successfully"
        );
    }

    public PageResponse<ProductResponse> getAllProductsByPage(Pageable pageable) {
        Page<ProductResponse> productPage=productRepository.findAll(pageable)
                .map(productMapper::toResponse);

        return PageResponse.<ProductResponse>builder()
                .content(productPage.getContent())
                .first(productPage.isFirst())
                .last(productPage.isLast())
                .size(productPage.getSize())
                .totalPages(productPage.getTotalPages())
                .totalElements(productPage.getTotalElements())
                .build();

    }

    public PageResponse<ProductResponse> searchProductsByTitle(Pageable pageable, String query) {
        Page<Product> productPage=productRepository.findByTitleContainingIgnoreCase(query, pageable);

        return PageResponse.<ProductResponse>builder()
                .content(productPage.getContent()
                        .stream().
                        map(productMapper::toResponse)
                        .collect(Collectors.toCollection(ArrayList::new)))
                .first(productPage.isFirst())
                .last(productPage.isLast())
                .size(productPage.getSize())
                .totalPages(productPage.getTotalPages())
                .totalElements(productPage.getTotalElements())
                .build();
    }
}


