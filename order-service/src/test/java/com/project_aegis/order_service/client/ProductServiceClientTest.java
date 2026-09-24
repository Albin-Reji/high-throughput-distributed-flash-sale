package com.project_aegis.order_service.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project_aegis.order_service.client.dto.SkuClientResponse;
import com.project_aegis.order_service.exception.ProductServiceClientException;
import com.project_aegis.order_service.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@DisplayName("ProductServiceClient Unit Tests")
class ProductServiceClientTest {

    private MockRestServiceServer mockServer;
    private ProductServiceClient client;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        RestClient.Builder restClientBuilder = RestClient.builder().baseUrl("http://localhost:8082");
        mockServer = MockRestServiceServer.bindTo(restClientBuilder).build();
        client = new ProductServiceClient(restClientBuilder.build());
    }

    @Nested
    @DisplayName("getSku Tests")
    class GetSkuTests {

        @Test
        @DisplayName("should successfully retrieve SKU snapshot")
        void shouldRetrieveSkuSuccessfully() throws Exception {
            UUID skuId = UUID.randomUUID();
            SkuClientResponse response = SkuClientResponse.builder()
                    .id(skuId)
                    .skuCode("SKU-001")
                    .productName("Test Product")
                    .price(new BigDecimal("999.00"))
                    .build();

            mockServer.expect(requestTo("http://localhost:8082/api/v1/products/skus/" + skuId))
                    .andExpect(method(HttpMethod.GET))
                    .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer test-token"))
                    .andRespond(withSuccess(objectMapper.writeValueAsString(response), MediaType.APPLICATION_JSON));

            SkuClientResponse result = client.getSku(skuId, "Bearer test-token");

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(skuId);
            assertThat(result.getPrice()).isEqualByComparingTo("999.00");
            assertThat(result.getProductName()).isEqualTo("Test Product");
            mockServer.verify();
        }

        @Test
        @DisplayName("should throw ProductServiceClientException when price is null")
        void shouldThrowWhenPriceIsNull() throws Exception {
            UUID skuId = UUID.randomUUID();
            SkuClientResponse response = SkuClientResponse.builder()
                    .id(skuId)
                    .skuCode("SKU-001")
                    .price(null)
                    .build();

            mockServer.expect(requestTo("http://localhost:8082/api/v1/products/skus/" + skuId))
                    .andExpect(method(HttpMethod.GET))
                    .andRespond(withSuccess(objectMapper.writeValueAsString(response), MediaType.APPLICATION_JSON));

            assertThatThrownBy(() -> client.getSku(skuId, null))
                    .isInstanceOf(ProductServiceClientException.class)
                    .hasMessageContaining("Product service returned no price for SKU: " + skuId);

            mockServer.verify();
        }
    }

    @Nested
    @DisplayName("getSkusBatch Tests")
    class GetSkusBatchTests {

        @Test
        @DisplayName("should return empty list when skuIds is null or empty")
        void shouldReturnEmptyListForEmptyInput() {
            assertThat(client.getSkusBatch(null, "token")).isEmpty();
            assertThat(client.getSkusBatch(Collections.emptyList(), "token")).isEmpty();
        }

        @Test
        @DisplayName("should fetch batch of SKUs in a single POST request")
        void shouldFetchBatchSuccessfully() throws Exception {
            UUID skuId1 = UUID.randomUUID();
            UUID skuId2 = UUID.randomUUID();
            List<UUID> skuIds = List.of(skuId1, skuId2);

            SkuClientResponse sku1 = SkuClientResponse.builder()
                    .id(skuId1)
                    .skuCode("SKU-001")
                    .productName("Product 1")
                    .price(new BigDecimal("1200.00"))
                    .build();

            SkuClientResponse sku2 = SkuClientResponse.builder()
                    .id(skuId2)
                    .skuCode("SKU-002")
                    .productName("Product 2")
                    .price(new BigDecimal("850.50"))
                    .build();

            mockServer.expect(requestTo("http://localhost:8082/api/v1/products/skus/batch"))
                    .andExpect(method(HttpMethod.POST))
                    .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer valid-token"))
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(content().json(objectMapper.writeValueAsString(skuIds)))
                    .andRespond(withSuccess(objectMapper.writeValueAsString(List.of(sku1, sku2)), MediaType.APPLICATION_JSON));

            List<SkuClientResponse> result = client.getSkusBatch(skuIds, "Bearer valid-token");

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getId()).isEqualTo(skuId1);
            assertThat(result.get(0).getPrice()).isEqualByComparingTo("1200.00");
            assertThat(result.get(1).getId()).isEqualTo(skuId2);
            assertThat(result.get(1).getPrice()).isEqualByComparingTo("850.50");
            mockServer.verify();
        }

        @Test
        @DisplayName("should default productName if null or blank in batch response")
        void shouldDefaultProductNameIfMissing() throws Exception {
            UUID skuId1 = UUID.randomUUID();
            SkuClientResponse sku1 = SkuClientResponse.builder()
                    .id(skuId1)
                    .skuCode("SKU-ABC")
                    .productName(null)
                    .price(new BigDecimal("100.00"))
                    .build();

            mockServer.expect(requestTo("http://localhost:8082/api/v1/products/skus/batch"))
                    .andExpect(method(HttpMethod.POST))
                    .andRespond(withSuccess(objectMapper.writeValueAsString(List.of(sku1)), MediaType.APPLICATION_JSON));

            List<SkuClientResponse> result = client.getSkusBatch(List.of(skuId1), null);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getProductName()).isEqualTo("Product (SKU-ABC)");
            mockServer.verify();
        }

        @Test
        @DisplayName("should throw ProductServiceClientException if any SKU in batch has null price")
        void shouldThrowIfBatchItemHasNullPrice() throws Exception {
            UUID skuId1 = UUID.randomUUID();
            SkuClientResponse sku1 = SkuClientResponse.builder()
                    .id(skuId1)
                    .skuCode("SKU-001")
                    .price(null)
                    .build();

            mockServer.expect(requestTo("http://localhost:8082/api/v1/products/skus/batch"))
                    .andExpect(method(HttpMethod.POST))
                    .andRespond(withSuccess(objectMapper.writeValueAsString(List.of(sku1)), MediaType.APPLICATION_JSON));

            assertThatThrownBy(() -> client.getSkusBatch(List.of(skuId1), null))
                    .isInstanceOf(ProductServiceClientException.class)
                    .hasMessageContaining("Product service returned no price for SKU: " + skuId1);

            mockServer.verify();
        }
    }
}
