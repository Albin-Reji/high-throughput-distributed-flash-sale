package com.aegis.product_service.service;

import com.aegis.product_service.dto.common.PageResponse;
import com.aegis.product_service.dto.common.ProductSuccessResponse;
import com.aegis.product_service.dto.request.ProductPatchRequest;
import com.aegis.product_service.dto.request.ProductRequest;
import com.aegis.product_service.dto.request.ProductUpdateRequest;
import com.aegis.product_service.dto.response.ProductAttributeResponse;
import com.aegis.product_service.dto.response.ProductResponse;
import com.aegis.product_service.dto.response.ProductSummaryResponse;
import com.aegis.product_service.dto.response.SkuResponse;
import com.aegis.product_service.entity.Category;
import com.aegis.product_service.entity.Product;
import com.aegis.product_service.entity.ProductAttribute;
import com.aegis.product_service.entity.Sku;
import com.aegis.product_service.exception.ResourceAlreadyExists;
import com.aegis.product_service.exception.ResourceNotFound;
import com.aegis.product_service.mapper.ProductMapper;
import com.aegis.product_service.repository.CategoryRepository;
import com.aegis.product_service.repository.ProductAttributeRepository;
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
    private final ProductAttributeRepository productAttributeRepository;
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

    /**
     * <p>Retrieves all products with pagination.</p>
     * @param page
     * @param size
     * @return {@link PageResponse} containing {@link ProductResponse}
     */
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

    /**
     * <p>Retrieves a product by its ID.</p>
     * @param id
     * @return {@link ProductResponse}
     */
    public ProductResponse getProductById(UUID id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFound("Product not found with id: " + id));

        return productMapper.toResponse(product);
    }

    /**
     * <p>Patches a product by its ID.</p>
     * @param id
     * @param request
     * @return {@link ProductSuccessResponse}
     */
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

    /**
     * <p>Deletes a product by its ID.</p>
     * @param id
     * @return {@link ProductSuccessResponse}
     */
    @Transactional
    public ProductSuccessResponse deleteProduct(UUID id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFound("Product not found with id: " + id));

        productRepository.delete(product);

        return new ProductSuccessResponse(
                product.getId(),
                product.getTitle(),
                "product deleted successfully"
        );
    }

    /**
     * <p>Retrieves a page of all products.</p>
     * @param pageable
     * @return {@link PageResponse} containing {@link ProductResponse}
     */
    public PageResponse<ProductResponse> getAllProductsByPage(Pageable pageable) {
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

    /**
     * <p>Searches for products by title.</p>
     * @param pageable
     * @param query
     * @return {@link PageResponse} containing {@link ProductResponse}
     */
    public PageResponse<ProductResponse> searchProductsByTitle(Pageable pageable, String query) {
        Page<Product> productPage = productRepository.findByTitleContainingIgnoreCase(query, pageable);

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

    /**
     * <p>Retrieves a page of products by category ID.</p>
     * @param pageable
     * @param categoryId
     * @return {@link PageResponse} containing {@link ProductSummaryResponse}
     */

    public PageResponse<ProductSummaryResponse> getProductsByCategory(Pageable pageable, UUID categoryId) {
        categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFound("Category not found with id: " + categoryId));
        Page<Product> productPage = productRepository.findByCategoryId(pageable, categoryId);

        return PageResponse.<ProductSummaryResponse>builder()
                .content(productPage.getContent()
                        .stream()
                        .map(productMapper::toSummaryResponse)
                        .collect(Collectors.toCollection(ArrayList::new)))
                .first(productPage.isFirst())
                .last(productPage.isLast())
                .size(productPage.getSize())
                .totalPages(productPage.getTotalPages())
                .totalElements(productPage.getTotalElements())
                .build();
    }

    /**
     * <p>Retrieves a page of SKUs by product ID.</p>
     * @param pageable
     * @param productId
     * @return {@link  SkuResponse}
     */
    public PageResponse<SkuResponse> getAllSkusByProductId(Pageable pageable, UUID productId) {
        productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFound("Product not found with id: " + productId));

        Page<Sku> skuPage = skuRepository.findByProductId(pageable, productId);

        return PageResponse.<SkuResponse>builder()
                .content(skuPage.getContent()
                        .stream()
                       .map(sku -> SkuResponse.builder()
                                .id(sku.getId())
                                .skuCode(sku.getSkuCode())
                                .color(sku.getColor())
                                .size(sku.getSize())
                                .price(sku.getPrice())
                                .build())
                .collect(Collectors.toCollection(ArrayList::new)))
                .first(skuPage.isFirst())
                .last(skuPage.isLast())
                .size(skuPage.getSize())
                .totalPages(skuPage.getTotalPages())
                .totalElements(skuPage.getTotalElements())
                .build();



    }

    /**
     * <p>Retrieves a page of product attributes by product ID.</p>
     * @param pageable
     * @param productId
     * @return {@link PageResponse<ProductAttributeResponse>}
     */
    public PageResponse<ProductAttributeResponse> getProductAttributesByProductId(Pageable pageable, UUID productId) {
        productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFound("Product not found with id: " + productId));

        Page<ProductAttribute> productAttributePage = productAttributeRepository.findByProductId(pageable, productId);

        return PageResponse.<ProductAttributeResponse>builder()
                .content(productAttributePage.getContent()
                        .stream()
                        .map(attr -> ProductAttributeResponse.builder()
                                .name(attr.getAttributeName())
                                .value(attr.getAttributeValue())
                                .build())
                        .collect(Collectors.toCollection(ArrayList::new)))
                .first(productAttributePage.isFirst())
                .last(productAttributePage.isLast())
                .size(productAttributePage.getSize())
                .totalPages(productAttributePage.getTotalPages())
                .totalElements(productAttributePage.getTotalElements())
                .build();
    }
}


