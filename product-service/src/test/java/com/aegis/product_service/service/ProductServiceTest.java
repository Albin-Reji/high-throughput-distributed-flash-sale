package com.aegis.product_service.service;

import com.aegis.product_service.dto.common.ProductAttributeSuccessResponse;
import com.aegis.product_service.dto.common.ProductSuccessResponse;
import com.aegis.product_service.dto.common.SkuSuccessResponse;
import com.aegis.product_service.dto.request.*;
import com.aegis.product_service.dto.response.ProductResponse;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductService Unit Tests")
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private SkuRepository skuRepository;

    @Mock
    private ProductAttributeRepository productAttributeRepository;

    @Mock
    private ProductMapper productMapper;

    @InjectMocks
    private ProductService productService;

    private UUID productId;
    private UUID categoryId;
    private Category category;
    private Product product;

    @BeforeEach
    void setUp() {
        productId = UUID.randomUUID();
        categoryId = UUID.randomUUID();

        category = Category.builder()
                .id(categoryId)
                .name("Electronics")
                .build();

        product = Product.builder()
                .id(productId)
                .title("MacBook Pro")
                .description("Apple laptop")
                .category(category)
                .skus(new ArrayList<>())
                .productAttributes(new ArrayList<>())
                .build();
    }

    // ──────────────────────────────────────────────────────────────
    //  createProduct
    // ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("createProduct")
    class CreateProduct {

        @Test
        @DisplayName("should create product successfully")
        void shouldCreateProduct() {
            SkuRequest skuRequest = new SkuRequest();
            skuRequest.setSkuCode("MB-PRO-001");
            skuRequest.setPrice(BigDecimal.valueOf(1999.99));

            ProductAttributeRequest attrRequest = new ProductAttributeRequest();
            attrRequest.setName("RAM");
            attrRequest.setValue("16GB");

            ProductRequest request = new ProductRequest();
            request.setCategoryId(categoryId);
            request.setTitle("MacBook Pro");
            request.setDescription("Apple laptop");
            request.setSkus(List.of(skuRequest));
            request.setProductAttributes(List.of(attrRequest));

            Sku sku = Sku.builder().skuCode("MB-PRO-001").build();
            ProductAttribute attr = ProductAttribute.builder().attributeName("RAM").attributeValue("16GB").build();

            when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
            when(skuRepository.existsBySkuCode("MB-PRO-001")).thenReturn(false);
            when(productMapper.toEntity(eq(skuRequest), any(Product.class))).thenReturn(sku);
            when(productMapper.toEntity(eq(attrRequest), any(Product.class))).thenReturn(attr);
            when(productRepository.save(any(Product.class))).thenReturn(product);

            ProductSuccessResponse result = productService.createProduct(request);

            assertThat(result.id()).isEqualTo(productId);
            assertThat(result.title()).isEqualTo("MacBook Pro");
            verify(productRepository).save(any(Product.class));
        }

        @Test
        @DisplayName("should throw ResourceNotFound when category does not exist")
        void shouldThrowWhenCategoryNotFound() {
            ProductRequest request = new ProductRequest();
            request.setCategoryId(categoryId);
            request.setSkus(List.of());
            request.setProductAttributes(List.of());

            when(categoryRepository.findById(categoryId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> productService.createProduct(request))
                    .isInstanceOf(ResourceNotFound.class)
                    .hasMessageContaining("Category not found");
        }

        @Test
        @DisplayName("should throw ResourceAlreadyExists when SKU code is duplicate")
        void shouldThrowWhenDuplicateSkuCode() {
            SkuRequest skuRequest = new SkuRequest();
            skuRequest.setSkuCode("EXISTING-SKU");

            ProductRequest request = new ProductRequest();
            request.setCategoryId(categoryId);
            request.setSkus(List.of(skuRequest));
            request.setProductAttributes(List.of());

            when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
            when(skuRepository.existsBySkuCode("EXISTING-SKU")).thenReturn(true);

            assertThatThrownBy(() -> productService.createProduct(request))
                    .isInstanceOf(ResourceAlreadyExists.class)
                    .hasMessageContaining("SKU code already exists");
        }
    }

    // ──────────────────────────────────────────────────────────────
    //  updateProduct
    // ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("updateProduct")
    class UpdateProduct {

        @Test
        @DisplayName("should update product successfully")
        void shouldUpdateProduct() {
            ProductUpdateRequest request = new ProductUpdateRequest();
            request.setTitle("MacBook Air");
            request.setDescription("Updated laptop");
            request.setCategoryId(categoryId);

            when(productRepository.findById(productId)).thenReturn(Optional.of(product));
            when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
            when(productRepository.save(product)).thenReturn(product);

            ProductSuccessResponse result = productService.updateProduct(productId, request);

            assertThat(result.message()).contains("updated");
            assertThat(product.getTitle()).isEqualTo("MacBook Air");
        }

        @Test
        @DisplayName("should throw ResourceNotFound when product does not exist")
        void shouldThrowWhenProductNotFound() {
            ProductUpdateRequest request = new ProductUpdateRequest();
            request.setCategoryId(categoryId);

            when(productRepository.findById(productId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> productService.updateProduct(productId, request))
                    .isInstanceOf(ResourceNotFound.class)
                    .hasMessageContaining("Product not found");
        }

        @Test
        @DisplayName("should throw ResourceNotFound when category does not exist")
        void shouldThrowWhenCategoryNotFound() {
            ProductUpdateRequest request = new ProductUpdateRequest();
            request.setCategoryId(categoryId);

            when(productRepository.findById(productId)).thenReturn(Optional.of(product));
            when(categoryRepository.findById(categoryId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> productService.updateProduct(productId, request))
                    .isInstanceOf(ResourceNotFound.class)
                    .hasMessageContaining("Category not found");
        }
    }

    // ──────────────────────────────────────────────────────────────
    //  getProductById
    // ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getProductById")
    class GetProductById {

        @Test
        @DisplayName("should return product when found")
        void shouldReturnProduct() {
            ProductResponse expectedResponse = ProductResponse.builder()
                    .id(productId)
                    .title("MacBook Pro")
                    .build();

            when(productRepository.findById(productId)).thenReturn(Optional.of(product));
            when(productMapper.toResponse(product)).thenReturn(expectedResponse);

            ProductResponse result = productService.getProductById(productId);

            assertThat(result.getId()).isEqualTo(productId);
            assertThat(result.getTitle()).isEqualTo("MacBook Pro");
        }

        @Test
        @DisplayName("should throw ResourceNotFound when product not found")
        void shouldThrowWhenNotFound() {
            when(productRepository.findById(productId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> productService.getProductById(productId))
                    .isInstanceOf(ResourceNotFound.class);
        }
    }

    // ──────────────────────────────────────────────────────────────
    //  deleteProduct
    // ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("deleteProduct")
    class DeleteProduct {

        @Test
        @DisplayName("should delete product successfully")
        void shouldDeleteProduct() {
            when(productRepository.findById(productId)).thenReturn(Optional.of(product));

            ProductSuccessResponse result = productService.deleteProduct(productId);

            assertThat(result.message()).contains("deleted");
            verify(productRepository).delete(product);
        }

        @Test
        @DisplayName("should throw ResourceNotFound when product not found")
        void shouldThrowWhenNotFound() {
            when(productRepository.findById(productId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> productService.deleteProduct(productId))
                    .isInstanceOf(ResourceNotFound.class);
        }
    }

    // ──────────────────────────────────────────────────────────────
    //  createSku
    // ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("createSku")
    class CreateSku {

        @Test
        @DisplayName("should create SKU successfully")
        void shouldCreateSku() {
            SkuRequest request = new SkuRequest();
            request.setSkuCode("NEW-SKU-001");
            request.setColor("Silver");
            request.setSize("14-inch");
            request.setPrice(BigDecimal.valueOf(1999.99));

            when(productRepository.findById(productId)).thenReturn(Optional.of(product));
            when(skuRepository.existsBySkuCode("NEW-SKU-001")).thenReturn(false);
            when(skuRepository.save(any(Sku.class))).thenAnswer(invocation -> {
                Sku saved = invocation.getArgument(0);
                saved.setId(UUID.randomUUID());
                return saved;
            });

            SkuSuccessResponse result = productService.createSku(productId, request);

            assertThat(result.skuCode()).isEqualTo("NEW-SKU-001");
            assertThat(result.message()).contains("created");
        }

        @Test
        @DisplayName("should throw ResourceNotFound when product does not exist")
        void shouldThrowWhenProductNotFound() {
            SkuRequest request = new SkuRequest();
            request.setSkuCode("SKU-001");

            when(productRepository.findById(productId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> productService.createSku(productId, request))
                    .isInstanceOf(ResourceNotFound.class);
        }

        @Test
        @DisplayName("should throw ResourceAlreadyExists when SKU code already exists")
        void shouldThrowWhenDuplicateSkuCode() {
            SkuRequest request = new SkuRequest();
            request.setSkuCode("EXISTING-SKU");

            when(productRepository.findById(productId)).thenReturn(Optional.of(product));
            when(skuRepository.existsBySkuCode("EXISTING-SKU")).thenReturn(true);

            assertThatThrownBy(() -> productService.createSku(productId, request))
                    .isInstanceOf(ResourceAlreadyExists.class);
        }
    }

    // ──────────────────────────────────────────────────────────────
    //  updateSku
    // ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("updateSku")
    class UpdateSku {

        private UUID skuId;

        @BeforeEach
        void setUp() {
            skuId = UUID.randomUUID();
        }

        @Test
        @DisplayName("should update SKU successfully")
        void shouldUpdateSku() {
            Sku sku = Sku.builder()
                    .id(skuId)
                    .skuCode("SKU-001")
                    .color("Silver")
                    .size("14-inch")
                    .price(BigDecimal.valueOf(1999.99))
                    .product(product)
                    .build();

            SkuUpdateRequest request = new SkuUpdateRequest();
            request.setColor("Space Gray");
            request.setPrice(BigDecimal.valueOf(2199.99));

            when(productRepository.findById(productId)).thenReturn(Optional.of(product));
            when(skuRepository.findByIdAndProductId(skuId, productId)).thenReturn(Optional.of(sku));
            when(skuRepository.save(sku)).thenReturn(sku);

            SkuSuccessResponse result = productService.updateSku(productId, skuId, request);

            assertThat(result.message()).contains("updated");
            assertThat(sku.getColor()).isEqualTo("Space Gray");
            assertThat(sku.getPrice()).isEqualTo(BigDecimal.valueOf(2199.99));
            assertThat(sku.getSize()).isEqualTo("14-inch"); // unchanged
        }

        @Test
        @DisplayName("should throw ResourceNotFound when product not found")
        void shouldThrowWhenProductNotFound() {
            SkuUpdateRequest request = new SkuUpdateRequest();
            when(productRepository.findById(productId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> productService.updateSku(productId, skuId, request))
                    .isInstanceOf(ResourceNotFound.class);
        }

        @Test
        @DisplayName("should throw ResourceNotFound when SKU not found")
        void shouldThrowWhenSkuNotFound() {
            SkuUpdateRequest request = new SkuUpdateRequest();
            when(productRepository.findById(productId)).thenReturn(Optional.of(product));
            when(skuRepository.findByIdAndProductId(skuId, productId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> productService.updateSku(productId, skuId, request))
                    .isInstanceOf(ResourceNotFound.class);
        }
    }

    // ──────────────────────────────────────────────────────────────
    //  deleteSkuBySkuId
    // ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("deleteSkuBySkuId")
    class DeleteSkuBySkuId {

        private UUID skuId;

        @BeforeEach
        void setUp() {
            skuId = UUID.randomUUID();
        }

        @Test
        @DisplayName("should delete SKU successfully")
        void shouldDeleteSku() {
            Sku sku = Sku.builder()
                    .id(skuId)
                    .skuCode("SKU-001")
                    .build();

            when(productRepository.findById(productId)).thenReturn(Optional.of(product));
            when(skuRepository.findById(skuId)).thenReturn(Optional.of(sku));

            SkuSuccessResponse result = productService.deleteSkuBySkuId(productId, skuId);

            assertThat(result.message()).contains("deleted");
            verify(skuRepository).delete(sku);
        }

        @Test
        @DisplayName("should throw ResourceNotFound when product not found")
        void shouldThrowWhenProductNotFound() {
            when(productRepository.findById(productId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> productService.deleteSkuBySkuId(productId, skuId))
                    .isInstanceOf(ResourceNotFound.class);
        }

        @Test
        @DisplayName("should throw ResourceNotFound when SKU not found")
        void shouldThrowWhenSkuNotFound() {
            when(productRepository.findById(productId)).thenReturn(Optional.of(product));
            when(skuRepository.findById(skuId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> productService.deleteSkuBySkuId(productId, skuId))
                    .isInstanceOf(ResourceNotFound.class);
        }
    }

    // ──────────────────────────────────────────────────────────────
    //  createProductAttribute
    // ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("createProductAttribute")
    class CreateProductAttribute {

        @Test
        @DisplayName("should create product attribute successfully")
        void shouldCreateAttribute() {
            ProductAttributeRequest request = new ProductAttributeRequest();
            request.setName("RAM");
            request.setValue("16GB");

            when(productRepository.findById(productId)).thenReturn(Optional.of(product));
            when(productAttributeRepository.findByProductIdAndAttributeName(productId, "RAM"))
                    .thenReturn(Optional.empty());

            ProductAttributeSuccessResponse result =
                    productService.createProductAttribute(productId, request);

            assertThat(result.getName()).isEqualTo("RAM");
            assertThat(result.getValue()).isEqualTo("16GB");
            assertThat(result.getMessage()).contains("created");
            verify(productAttributeRepository).save(any(ProductAttribute.class));
        }

        @Test
        @DisplayName("should throw ResourceAlreadyExists when attribute already exists")
        void shouldThrowWhenDuplicate() {
            ProductAttributeRequest request = new ProductAttributeRequest();
            request.setName("RAM");
            request.setValue("16GB");

            ProductAttribute existingAttr = ProductAttribute.builder()
                    .attributeName("RAM")
                    .attributeValue("8GB")
                    .product(product)
                    .build();

            when(productRepository.findById(productId)).thenReturn(Optional.of(product));
            when(productAttributeRepository.findByProductIdAndAttributeName(productId, "RAM"))
                    .thenReturn(Optional.of(existingAttr));

            assertThatThrownBy(() -> productService.createProductAttribute(productId, request))
                    .isInstanceOf(ResourceAlreadyExists.class)
                    .hasMessageContaining("already exists");
        }

        @Test
        @DisplayName("should throw ResourceNotFound when product not found")
        void shouldThrowWhenProductNotFound() {
            ProductAttributeRequest request = new ProductAttributeRequest();
            request.setName("RAM");
            request.setValue("16GB");

            when(productRepository.findById(productId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> productService.createProductAttribute(productId, request))
                    .isInstanceOf(ResourceNotFound.class);
        }
    }

    // ──────────────────────────────────────────────────────────────
    //  isSkuExist
    // ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("isSkuExist")
    class IsSkuExist {

        @Test
        @DisplayName("should return SkuResponse when SKU exists")
        void shouldReturnSkuWhenExists() {
            UUID skuId = UUID.randomUUID();
            Sku sku = Sku.builder()
                    .id(skuId)
                    .skuCode("SKU-001")
                    .color("Silver")
                    .size("14-inch")
                    .price(BigDecimal.valueOf(1999.99))
                    .build();

            when(skuRepository.findById(skuId)).thenReturn(Optional.of(sku));

            SkuResponse result = productService.isSkuExist(skuId);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(skuId);
            assertThat(result.getSkuCode()).isEqualTo("SKU-001");
        }

        @Test
        @DisplayName("should return null when SKU does not exist")
        void shouldReturnNullWhenNotExists() {
            UUID skuId = UUID.randomUUID();
            when(skuRepository.findById(skuId)).thenReturn(Optional.empty());

            SkuResponse result = productService.isSkuExist(skuId);

            assertThat(result).isNull();
        }
    }
}
