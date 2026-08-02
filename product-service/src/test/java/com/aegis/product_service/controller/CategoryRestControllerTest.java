package com.aegis.product_service.controller;

import com.aegis.product_service.controller.admin.AdminCategoryController;
import com.aegis.product_service.controller.publicapi.PublicCategoryController;
import com.aegis.product_service.dto.request.CategoryRequest;
import com.aegis.product_service.dto.response.CategoryResponse;
import com.aegis.product_service.dto.response.CategoryTreeResponse;
import com.aegis.product_service.dto.common.PageResponse;
import com.aegis.product_service.exception.GlobalExceptionHandler;
import com.aegis.product_service.exception.ResourceAlreadyExists;
import com.aegis.product_service.exception.ResourceNotFound;
import com.aegis.product_service.service.CategoryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({PublicCategoryController.class, AdminCategoryController.class})
@Import(GlobalExceptionHandler.class)
class CategoryRestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CategoryService categoryService;

    @Test
    void createCategory_withNullParentCategoryId_shouldReturn200() throws Exception {
        UUID generatedId = UUID.randomUUID();
        CategoryResponse response = CategoryResponse.builder()
                .id(generatedId)
                .name("Electronics")
                .parentCategoryId(null)
                .parentCategoryName(null)
                .createdAt(LocalDateTime.now())
                .build();

        given(categoryService.createCategory(any(CategoryRequest.class))).willReturn(response);

        String jsonContent = """
                {
                  "name": "Electronics",
                  "parentCategoryId": null
                }
                """;

        mockMvc.perform(post("/api/v1/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonContent))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(generatedId.toString()))
                .andExpect(jsonPath("$.name").value("Electronics"))
                .andExpect(jsonPath("$.parentCategoryId").isEmpty());
    }

    @Test
    void createCategory_whenNameAlreadyExists_shouldReturn409() throws Exception {
        given(categoryService.createCategory(any(CategoryRequest.class)))
                .willThrow(new ResourceAlreadyExists("Category with name 'Electronics' already exists."));

        String jsonContent = """
                {
                  "name": "Electronics",
                  "parentCategoryId": null
                }
                """;

        mockMvc.perform(post("/api/v1/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonContent))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Category with name 'Electronics' already exists."))
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void createCategory_withBlankName_shouldReturn400() throws Exception {
        String jsonContent = """
                {
                  "name": "",
                  "parentCategoryId": null
                }
                """;

        mockMvc.perform(post("/api/v1/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonContent))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAllCategories_shouldReturn200() throws Exception {
        CategoryResponse response = CategoryResponse.builder()
                .id(UUID.randomUUID())
                .name("Electronics")
                .createdAt(LocalDateTime.now())
                .build();

        given(categoryService.getAllCategories()).willReturn(List.of(response));

        mockMvc.perform(get("/api/v1/categories/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Electronics"));
    }

    @Test
    void getCategoryTree_shouldReturn200() throws Exception {
        CategoryTreeResponse tree = new CategoryTreeResponse();
        tree.setId(UUID.randomUUID());
        tree.setName("Electronics");
        tree.setChildren(List.of());

        given(categoryService.getCategoryTree()).willReturn(List.of(tree));

        mockMvc.perform(get("/api/v1/categories/tree"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Electronics"))
                .andExpect(jsonPath("$[0].children").isArray());
    }

    @Test
    void getCategoryByPage_shouldReturn200() throws Exception {
        CategoryResponse category = CategoryResponse.builder()
                .id(UUID.randomUUID())
                .name("Electronics")
                .createdAt(LocalDateTime.now())
                .build();

        PageResponse<CategoryResponse> pageResponse = PageResponse.<CategoryResponse>builder()
                .content(List.of(category))
                .page(0)
                .size(10)
                .totalElements(1)
                .totalPages(1)
                .first(true)
                .last(true)
                .build();

        given(categoryService.getCategoryByPage(0, 10)).willReturn(pageResponse);

        mockMvc.perform(get("/api/v1/categories")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Electronics"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void getCategoryById_shouldReturn200() throws Exception {
        UUID id = UUID.randomUUID();
        CategoryResponse response = CategoryResponse.builder()
                .id(id)
                .name("Electronics")
                .createdAt(LocalDateTime.now())
                .build();

        given(categoryService.getCategoryById(id)).willReturn(response);

        mockMvc.perform(get("/api/v1/categories/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.name").value("Electronics"));
    }

    @Test
    void getCategoryById_whenNotFound_shouldReturn404() throws Exception {
        UUID id = UUID.randomUUID();
        given(categoryService.getCategoryById(id))
                .willThrow(new ResourceNotFound("Category not found with id: " + id));

        mockMvc.perform(get("/api/v1/categories/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Category not found with id: " + id))
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void updateCategoryById_shouldReturn200() throws Exception {
        UUID id = UUID.randomUUID();
        UUID parentId = UUID.randomUUID();
        CategoryResponse response = CategoryResponse.builder()
                .id(id)
                .name("Gadgets")
                .parentCategoryId(parentId)
                .parentCategoryName("Electronics")
                .createdAt(LocalDateTime.now())
                .build();

        given(categoryService.putCategory(eq(id), any(CategoryRequest.class))).willReturn(response);

        String jsonContent = """
                {
                  "name": "Gadgets",
                  "parentCategoryId": "%s"
                }
                """.formatted(parentId);

        mockMvc.perform(put("/api/v1/admin/categories/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonContent))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Gadgets"))
                .andExpect(jsonPath("$.parentCategoryId").value(parentId.toString()));
    }

    @Test
    void patchCategory_shouldReturn200() throws Exception {
        UUID id = UUID.randomUUID();
        CategoryResponse response = CategoryResponse.builder()
                .id(id)
                .name("Updated Electronics")
                .createdAt(LocalDateTime.now())
                .build();

        given(categoryService.updateCategory(eq(id), any(CategoryRequest.class))).willReturn(response);

        String jsonContent = """
                {
                  "name": "Updated Electronics"
                }
                """;

        mockMvc.perform(patch("/api/v1/admin/categories/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonContent))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Electronics"));
    }

    @Test
    void deleteCategory_shouldReturn204() throws Exception {
        UUID id = UUID.randomUUID();
        doNothing().when(categoryService).deleteCategory(id);

        mockMvc.perform(delete("/api/v1/admin/categories/{id}", id))
                .andExpect(status().isNoContent());

        verify(categoryService).deleteCategory(id);
    }

    @Test
    void searchCategory_shouldReturn200() throws Exception {
        CategoryResponse response = CategoryResponse.builder()
                .id(UUID.randomUUID())
                .name("Electronics")
                .createdAt(LocalDateTime.now())
                .build();

        given(categoryService.searchCategories("Electro")).willReturn(List.of(response));

        mockMvc.perform(get("/api/v1/categories/search")
                        .param("name", "Electro"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Electronics"));
    }
}
