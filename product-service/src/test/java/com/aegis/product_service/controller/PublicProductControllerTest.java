package com.aegis.product_service.controller;

import com.aegis.product_service.controller.publicapi.PublicProductController;
import com.aegis.product_service.dto.response.SkuResponse;
import com.aegis.product_service.exception.GlobalExceptionHandler;
import com.aegis.product_service.service.ProductService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PublicProductController.class)
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("PublicProductController Batch SKU Tests")
class PublicProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private ProductService productService;

    @Test
    @DisplayName("POST /api/v1/products/skus/batch should return 200 and list of SKU snapshots")
    void getSkusBatch_withListOfSkuIds_shouldReturn200AndSkuResponses() throws Exception {
        UUID skuId1 = UUID.randomUUID();
        UUID skuId2 = UUID.randomUUID();

        SkuResponse sku1 = SkuResponse.builder()
                .id(skuId1)
                .skuCode("IPHONE-15-BLK")
                .color("Black")
                .size("128GB")
                .price(BigDecimal.valueOf(799.99))
                .productName("iPhone 15")
                .build();

        SkuResponse sku2 = SkuResponse.builder()
                .id(skuId2)
                .skuCode("IPHONE-15-BLU")
                .color("Blue")
                .size("256GB")
                .price(BigDecimal.valueOf(899.99))
                .productName("iPhone 15")
                .build();

        given(productService.getSkusBatch(any())).willReturn(List.of(sku1, sku2));

        List<UUID> request = List.of(skuId1, skuId2);

        mockMvc.perform(post("/api/v1/products/skus/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").value(skuId1.toString()))
                .andExpect(jsonPath("$[0].skuCode").value("IPHONE-15-BLK"))
                .andExpect(jsonPath("$[0].color").value("Black"))
                .andExpect(jsonPath("$[0].size").value("128GB"))
                .andExpect(jsonPath("$[0].price").value(799.99))
                .andExpect(jsonPath("$[0].productName").value("iPhone 15"))
                .andExpect(jsonPath("$[1].id").value(skuId2.toString()))
                .andExpect(jsonPath("$[1].skuCode").value("IPHONE-15-BLU"))
                .andExpect(jsonPath("$[1].productName").value("iPhone 15"));
    }

    @Test
    @DisplayName("POST /api/v1/products/skus/batch with empty array should return 200 and empty list")
    void getSkusBatch_withEmptyList_shouldReturn200AndEmptyList() throws Exception {
        given(productService.getSkusBatch(any())).willReturn(List.of());

        mockMvc.perform(post("/api/v1/products/skus/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("POST /api/v1/products/skus/batch with null/empty body should return 200 and empty list")
    void getSkusBatch_withEmptyBody_shouldReturn200AndEmptyList() throws Exception {
        given(productService.getSkusBatch(null)).willReturn(List.of());

        mockMvc.perform(post("/api/v1/products/skus/batch")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("GET /api/v1/products/skus/{skuId} should return single SKU snapshot")
    void isSkuExist_shouldReturnSkuSnapshot() throws Exception {
        UUID skuId = UUID.randomUUID();
        SkuResponse sku = SkuResponse.builder()
                .id(skuId)
                .skuCode("MACBOOK-M3")
                .color("Space Gray")
                .size("14-inch")
                .price(BigDecimal.valueOf(1999.00))
                .build();

        given(productService.isSkuExist(skuId)).willReturn(sku);

        mockMvc.perform(get("/api/v1/products/skus/{skuId}", skuId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(skuId.toString()))
                .andExpect(jsonPath("$.skuCode").value("MACBOOK-M3"))
                .andExpect(jsonPath("$.price").value(1999.00));
    }
}
