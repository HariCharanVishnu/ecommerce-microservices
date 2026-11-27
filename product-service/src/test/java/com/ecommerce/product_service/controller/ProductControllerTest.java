
package com.ecommerce.product_service.controller;

import com.ecommerce.product_service.dto.ProductRequest;
import com.ecommerce.product_service.dto.ProductResponse;
import com.ecommerce.product_service.exception.DuplicateSkuException;
import com.ecommerce.product_service.exception.GlobalExceptionHandler;
import com.ecommerce.product_service.exception.ProductNotFoundException;
import com.ecommerce.product_service.service.ProductService;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductController.class)
@Import(GlobalExceptionHandler.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ProductService productService;

    @Test
    void createProduct_shouldReturnCreatedStatus() throws Exception {
        ProductRequest request = new ProductRequest();
        request.setSku("TEST-001");
        request.setName("Test Product");
        request.setPrice(new BigDecimal("1000.00"));
        request.setCategory("Electronics");

        ProductResponse response = new ProductResponse();
        response.setId(1L);
        response.setSku("TEST-001");
        response.setName("Test Product");

        when(productService.saveProduct(any(ProductRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.sku").value("TEST-001"))
                .andExpect(jsonPath("$.name").value("Test Product"));

        verify(productService).saveProduct(any(ProductRequest.class));
    }

    @Test
    void getProductById_shouldReturnProduct() throws Exception {
        ProductResponse response = new ProductResponse();
        response.setId(1L);
        response.setName("Test Product");
        response.setSku("TEST-001");

        when(productService.getProductById(1L)).thenReturn(response);

        mockMvc.perform(get("/api/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Test Product"))
                .andExpect(jsonPath("$.sku").value("TEST-001"));

        verify(productService).getProductById(1L);
    }

    @Test
    void getProductById_shouldReturnNotFound() throws Exception {
        when(productService.getProductById(99L))
                .thenThrow(new ProductNotFoundException(
                        "Product not found with ID: 99"));

        mockMvc.perform(get("/api/products/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));

        verify(productService).getProductById(99L);
    }

    @Test
    void createProduct_shouldReturnBadRequest_whenValidationFails()
            throws Exception {
        ProductRequest request = new ProductRequest();
        request.setSku("");
        request.setName("");
        request.setPrice(BigDecimal.ZERO);
        request.setCategory("");

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(productService);
    }

    @Test
    void createProduct_shouldReturnConflict_whenSkuAlreadyExists()
            throws Exception {
        ProductRequest request = new ProductRequest();
        request.setSku("PHONE-001");
        request.setName("Samsung");
        request.setPrice(new BigDecimal("50000.00"));
        request.setCategory("Electronics");

        when(productService.saveProduct(any(ProductRequest.class)))
                .thenThrow(new DuplicateSkuException(
                        "Product already exists with SKU: PHONE-001"));

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));

        verify(productService).saveProduct(any(ProductRequest.class));
    }


    @Test
    void getAllProducts_shouldReturnPaginatedProducts() throws Exception {
        ProductResponse product = new ProductResponse();
        product.setId(1L);
        product.setName("Samsung Galaxy");
        product.setSku("PHONE-001");

        Page<ProductResponse> page = new PageImpl<>(
                List.of(product),
                PageRequest.of(0, 10),
                1
        );

        when(productService.getAllProducts(0, 10, "id", "asc"))
                .thenReturn(page);

        mockMvc.perform(get("/api/products")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name")
                        .value("Samsung Galaxy"))
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(productService).getAllProducts(0, 10, "id", "asc");
    }


    @Test
    void getAllProducts_shouldSupportSorting() throws Exception {
        when(productService.getAllProducts(0, 5, "price", "desc"))
                .thenReturn(Page.empty(PageRequest.of(0, 5)));

        mockMvc.perform(get("/api/products")
                        .param("page", "0")
                        .param("size", "5")
                        .param("sortBy", "price")
                        .param("direction", "desc"))
                .andExpect(status().isOk());

        verify(productService).getAllProducts(0, 5, "price", "desc");
    }


    @Test
    void updateProduct_shouldReturnUpdatedProduct() throws Exception {
        ProductRequest request = new ProductRequest();
        request.setSku("PHONE-001");
        request.setName("Samsung Galaxy S25");
        request.setPrice(new BigDecimal("60000.00"));
        request.setCategory("Electronics");
        request.setBrand("Samsung");

        ProductResponse response = new ProductResponse();
        response.setId(1L);
        response.setSku("PHONE-001");
        response.setName("Samsung Galaxy S25");
        response.setBrand("Samsung");

        when(productService.updateProduct(eq(1L), any(ProductRequest.class)))
                .thenReturn(response);

        mockMvc.perform(put("/api/products/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Samsung Galaxy S25"))
                .andExpect(jsonPath("$.brand").value("Samsung"));

        verify(productService).updateProduct(eq(1L), any(ProductRequest.class));
    }


    @Test
    void updateProduct_shouldReturnNotFound() throws Exception {
        ProductRequest request = new ProductRequest();
        request.setSku("PHONE-001");
        request.setName("Samsung");
        request.setPrice(new BigDecimal("50000.00"));
        request.setCategory("Electronics");

        when(productService.updateProduct(eq(99L), any(ProductRequest.class)))
                .thenThrow(new ProductNotFoundException(
                        "Product not found with ID: 99"));

        mockMvc.perform(put("/api/products/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());

        verify(productService).updateProduct(eq(99L), any(ProductRequest.class));
    }


    @Test
    void deleteProduct_shouldReturnNoContent() throws Exception {
        doNothing().when(productService).deleteProduct(1L);

        mockMvc.perform(delete("/api/products/1"))
                .andExpect(status().isNoContent());

        verify(productService).deleteProduct(1L);
    }


    @Test
    void deleteProduct_shouldReturnNotFound() throws Exception {
        doThrow(new ProductNotFoundException(
                "Product not found with ID: 99"))
                .when(productService).deleteProduct(99L);

        mockMvc.perform(delete("/api/products/99"))
                .andExpect(status().isNotFound());

        verify(productService).deleteProduct(99L);
    }


    @Test
    void getAllProducts_shouldReturnBadRequest_whenPageIsNegative()
            throws Exception {

        mockMvc.perform(get("/api/products")
                        .param("page", "-1"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(productService);
    }

}
