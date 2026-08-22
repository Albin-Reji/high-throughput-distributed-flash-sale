package com.aegis.product_service.service;

import com.aegis.product_service.dto.common.PageResponse;
import com.aegis.product_service.dto.common.ProductAttributeSuccessResponse;
import com.aegis.product_service.dto.common.ProductSuccessResponse;
import com.aegis.product_service.dto.common.SkuSuccessResponse;
import com.aegis.product_service.dto.request.*;
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

    public static final String CATEGORY_NOT_FOUND_WITH_ID = "Category not found with id: ";
    public static final String SKU_NOT_FOUND_WITH_ID = "Sku not found with id: ";
    public static final String PRODUCT_NOT_FOUND_WITH_ID = "Product not found with id: ";

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final SkuRepository skuRepository;
    private final ProductAttributeRepository productAttributeRepository;
    private final ProductMapper productMapper;

    /**
     * <p>Creates a new product.</p>
     *
     * @param request ProductRequest containing the product data to create. Must include title, description,
     *                categoryId, list of productAttributes and list of skus. The skus skuCode values must be unique.
     * @return {@link ProductSuccessResponse} with the created product id, title and a success message
     */
    @Transactional
    public ProductSuccessResponse createProduct(@Valid ProductRequest request) {
        // check if category already exist
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFound(CATEGORY_NOT_FOUND_WITH_ID + request.getCategoryId()));
        // check for skucode is already exist
        request.getSkus()
                .forEach(skuRequest -> {
                    if (skuRepository.existsBySkuCode(skuRequest.getSkuCode())) {
                        throw new ResourceAlreadyExists("SKU code already exists: " + skuRequest.getSkuCode());
                    }
                });

        Product product = Product.builder()
                .category(category)
                .title(request.getTitle())
                .description(request.getDescription())
                .build();

        List<ProductAttribute> productAttributes = request.getProductAttributes()
                .stream()
                .map(prodAttr -> productMapper.toEntity(prodAttr, product))
                .collect(Collectors.toCollection(ArrayList::new));

        List<Sku> skus = request.getSkus()
                .stream()
                .map(skuRequest -> productMapper.toEntity(skuRequest, product))
                .collect(Collectors.toCollection(ArrayList::new));

        product.setProductAttributes(productAttributes);
        product.setSkus(skus);

        Product savedProduct = productRepository.save(product);
        return new ProductSuccessResponse(savedProduct.getId(), savedProduct.getTitle(), "product created successfully");
    }

    /**
     * <p>Updates a product by its ID.</p>
     *
     * @param id      UUID of the product to update
     * @param request ProductUpdateRequest containing new values for title, description and categoryId
     * @return {@link ProductSuccessResponse} with the product id, title and a success message
     */
    @Transactional
    public ProductSuccessResponse updateProduct(UUID id, @Valid ProductUpdateRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFound(PRODUCT_NOT_FOUND_WITH_ID + id));

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFound(CATEGORY_NOT_FOUND_WITH_ID + request.getCategoryId()));

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
     *
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
     *
     * @param id
     * @return {@link ProductResponse}
     */
    public ProductResponse getProductById(UUID id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFound(PRODUCT_NOT_FOUND_WITH_ID + id));

        return productMapper.toResponse(product);
    }

    /**
     * <p>Patches a product by its ID.</p>
     * <p>Only non-null fields in {@code request} will be applied to the existing product.</p>
     *
     * @param id      UUID of the product to patch
     * @param request ProductPatchRequest containing fields to update (nullable fields will be ignored)
     * @return {@link ProductSuccessResponse} with the product id, title and a success message
     */
    @Transactional
    public ProductSuccessResponse patchProduct(UUID id, @Valid ProductPatchRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFound(PRODUCT_NOT_FOUND_WITH_ID + id));

        Category requestCategory = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFound(CATEGORY_NOT_FOUND_WITH_ID + request.getCategoryId()));

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
     *
     * @param id UUID of the product to delete
     * @return {@link ProductSuccessResponse} containing the deleted product id, title and a success message
     */
    @Transactional
    public ProductSuccessResponse deleteProduct(UUID id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFound(PRODUCT_NOT_FOUND_WITH_ID + id));

        productRepository.delete(product);

        return new ProductSuccessResponse(
                product.getId(),
                product.getTitle(),
                "product deleted successfully"
        );
    }

    /**
     * <p>Retrieves a page of all products.</p>
     *
     * @param pageable Pageable instance (contains page number, size and sort information)
     * @return {@link PageResponse} containing {@link ProductResponse} for the provided pageable
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
     *
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
     *
     * @param pageable
     * @param categoryId
     * @return {@link PageResponse} containing {@link ProductSummaryResponse}
     */

    public PageResponse<ProductSummaryResponse> getProductsByCategory(Pageable pageable, UUID categoryId) {
        categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFound(CATEGORY_NOT_FOUND_WITH_ID + categoryId));
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
     *
     * @param pageable
     * @param productId
     * @return {@link  SkuResponse}
     */
    public PageResponse<SkuResponse> getAllSkusByProductId(Pageable pageable, UUID productId) {
        productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFound(PRODUCT_NOT_FOUND_WITH_ID + productId));

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
     *
     * @param pageable  Pageable instance for paging and sorting
     * @param productId UUID of the product whose attributes should be returned
     * @return {@link PageResponse<ProductAttributeResponse>} containing attribute name/value pairs
     */
    public PageResponse<ProductAttributeResponse> getProductAttributesByProductId(Pageable pageable, UUID productId) {
        productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFound(PRODUCT_NOT_FOUND_WITH_ID + productId));

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

    /**
     * <p>Creates a new SKU for the specified product.</p>
     *
     * @param productId UUID of the product to which the SKU will be added
     * @param request   SkuRequest containing skuCode, color, size and price for the new SKU
     * @return {@link SkuSuccessResponse} with the created SKU id, skuCode and a success message
     */
    @Transactional
    public SkuSuccessResponse createSku(UUID productId, SkuRequest request) {

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFound(PRODUCT_NOT_FOUND_WITH_ID + productId));

        if (skuRepository.existsBySkuCode(request.getSkuCode())) {
            throw new ResourceAlreadyExists("SKU code already exists: " + request.getSkuCode());
        }

        Sku sku = Sku.builder()
                .skuCode(request.getSkuCode())
                .color(request.getColor())
                .size(request.getSize())
                .price(request.getPrice())
                .product(product)
                .build();

        product.getSkus().add(sku);

        skuRepository.save(sku);
        return SkuSuccessResponse.builder()
                .id(sku.getId())
                .skuCode(sku.getSkuCode())
                .message("SKU created successfully")
                .build();
    }

    /**
     * <p>Updates an existing SKU for the specified product.</p>
     *
     * @param productId the ID of the product containing the SKU
     * @param id        the ID of the SKU to update
     * @param request   the SKU update request details
     * @return {@link SkuSuccessResponse} containing the updated SKU information
     */
    @Transactional
    public SkuSuccessResponse updateSku(UUID productId, UUID id, @Valid SkuUpdateRequest request) {

        productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFound(PRODUCT_NOT_FOUND_WITH_ID + productId));

        Sku sku = skuRepository.findByIdAndProductId(id, productId)
                .orElseThrow(() -> new ResourceNotFound(SKU_NOT_FOUND_WITH_ID + id));

        sku.setColor(request.getColor() != null ? request.getColor() : sku.getColor());
        sku.setSize(request.getSize() != null ? request.getSize() : sku.getSize());
        sku.setPrice(request.getPrice() != null ? request.getPrice() : sku.getPrice());

        skuRepository.save(sku);
        return SkuSuccessResponse.builder()
                .id(sku.getId())
                .skuCode(sku.getSkuCode())
                .message("SKU updated successfully")
                .build();


    }

    /**
     *
     * @param productId the ID of the product containing the SKU
     * @param skuId     skuId
     * @return {@link  SkuSuccessResponse}
     */
    @Transactional
    public SkuSuccessResponse deleteSkuBySkuId(UUID productId, UUID skuId) {
        productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFound(PRODUCT_NOT_FOUND_WITH_ID + productId));
        Sku sku = skuRepository.findById(skuId)
                .orElseThrow(() -> new ResourceNotFound(SKU_NOT_FOUND_WITH_ID + skuId));

        skuRepository.delete(sku);

        return SkuSuccessResponse.builder()
                .id(sku.getId())
                .skuCode(sku.getSkuCode())
                .message("SKU deleted successfully")
                .build();
    }

    public ProductAttributeSuccessResponse createProductAttribute(UUID productId, @Valid ProductAttributeRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFound(PRODUCT_NOT_FOUND_WITH_ID + productId));

        productAttributeRepository.findByProductIdAndAttributeName(productId, request.getName())
                .ifPresent(attr -> {
                    throw new ResourceAlreadyExists("Product attribute already exists with name: " + request.getName());
                });

        ProductAttribute productAttribute = ProductAttribute.builder()
                .attributeName(request.getName())
                .attributeValue(request.getValue())
                .product(product)
                .build();

        productAttributeRepository.save(productAttribute);
        return ProductAttributeSuccessResponse.builder()
                .name(productAttribute.getAttributeName())
                .value(productAttribute.getAttributeValue())
                .message("Product attribute created successfully")
                .build();
    }

    @Transactional
    public ProductAttributeSuccessResponse updateProductAttribute(UUID productId, String attributeName, ProductAttributeUpdateRequest request) {

        ProductAttribute productAttribute = productAttributeRepository.findByProductIdAndAttributeName(productId, attributeName)
                .orElseThrow(() -> new ResourceNotFound("Product attribute not found with name: " + attributeName));

        productAttribute.setAttributeValue(request.getValue() != null ? request.getValue() : productAttribute.getAttributeValue());

        return ProductAttributeSuccessResponse.builder()
                .name(productAttribute.getAttributeName())
                .value(productAttribute.getAttributeValue())
                .message("Product attribute updated successfully")
                .build();


    }

    @Transactional
    public ProductAttributeSuccessResponse deleteProductAttribute(UUID productId, String attributeName) {

        ProductAttribute productAttribute= productAttributeRepository.findByProductIdAndAttributeName(productId, attributeName)
                .orElseThrow(() -> new ResourceNotFound("Product attribute not found with name: " + attributeName + "  productId: "+ productId));

        productAttributeRepository.delete(productAttribute);

        return ProductAttributeSuccessResponse.builder()
                .name(productAttribute.getAttributeName())
                .value(productAttribute.getAttributeValue())
                .message("Product attribute deleted successfully")
                .build();
    }

    public SkuResponse isSkuExist(UUID skuId) {
        Sku sku=skuRepository.findById(skuId)
                .orElse(null);

        return sku==null?null : SkuResponse.builder()
                .id(sku.getId())
                .color(sku.getColor())
                .size(sku.getSize())
                .price(sku.getPrice())
                .skuCode(sku.getSkuCode())
                .build();

    }
}


