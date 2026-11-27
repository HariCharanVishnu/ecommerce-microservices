
package com.ecommerce.product_service.service;

import com.ecommerce.product_service.entity.Product;
import com.ecommerce.product_service.repository.ProductRepository;
import com.ecommerce.product_service.dto.ProductRequest;
import com.ecommerce.product_service.dto.ProductResponse;
import com.ecommerce.product_service.exception.DuplicateSkuException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.ecommerce.product_service.exception.ProductNotFoundException;

import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    @Test
    void shouldCreateProductSuccessfully() {

        // Arrange: Prepare request
        ProductRequest request = new ProductRequest();
        request.setSku("TEST-001");
        request.setName("Test Product");
        request.setDescription("Test Description");
        request.setPrice(new BigDecimal("1000.00"));
        request.setCategory("Electronics");
        request.setBrand("Test Brand");

        Product savedProduct = new Product();
        savedProduct.setId(1L);
        savedProduct.setSku("TEST-001");
        savedProduct.setName("Test Product");
        savedProduct.setDescription("Test Description");
        savedProduct.setPrice(new BigDecimal("1000.00"));
        savedProduct.setCategory("Electronics");
        savedProduct.setBrand("Test Brand");
        savedProduct.setActive(true);

        when(productRepository.existsBySku("TEST-001"))
                .thenReturn(false);

        when(productRepository.save(any(Product.class)))
                .thenReturn(savedProduct);

        // Act: Call service method
        ProductResponse response = productService.saveProduct(request);

        // Assert: Verify result
        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("TEST-001", response.getSku());
        assertEquals("Test Product", response.getName());
        assertEquals("Test Brand", response.getBrand());
        assertEquals(new BigDecimal("1000.00"), response.getPrice());

        verify(productRepository).existsBySku("TEST-001");
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void saveProduct_shouldThrowException_whenSkuAlreadyExists() {
        ProductRequest request = new ProductRequest();
        request.setSku("PHONE-001");

        when(productRepository.existsBySku("PHONE-001"))
                .thenReturn(true);

        assertThrows(DuplicateSkuException.class,
                () -> productService.saveProduct(request));

        verify(productRepository, never()).save(any(Product.class));
    }


    @Test
    void getProductById_shouldReturnProduct_whenProductExists() {
        Product product = new Product();
        product.setId(1L);
        product.setSku("PHONE-001");
        product.setName("Samsung Galaxy");
        product.setPrice(new BigDecimal("50000.00"));
        product.setCategory("Electronics");

        when(productRepository.findById(1L))
                .thenReturn(Optional.of(product));

        ProductResponse response = productService.getProductById(1L);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("PHONE-001", response.getSku());
        assertEquals("Samsung Galaxy", response.getName());

        verify(productRepository).findById(1L);
    }

    @Test
    void getProductById_shouldThrowException_whenProductDoesNotExist() {
        when(productRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThrows(ProductNotFoundException.class,
                () -> productService.getProductById(99L));

        verify(productRepository).findById(99L);
    }

    @Test
    void getProductBySku_shouldReturnProduct_whenSkuExists() {
        Product product = new Product();
        product.setId(2L);
        product.setSku("LAPTOP-001");
        product.setName("Dell Inspiron");
        product.setPrice(new BigDecimal("65000.00"));

        when(productRepository.findBySku("LAPTOP-001"))
                .thenReturn(Optional.of(product));

        ProductResponse response =
                productService.getProductBySku("LAPTOP-001");

        assertNotNull(response);
        assertEquals("LAPTOP-001", response.getSku());
        assertEquals("Dell Inspiron", response.getName());

        verify(productRepository).findBySku("LAPTOP-001");
    }

    @Test
    void getProductsByCategory_shouldReturnMatchingProducts() {
        Product product1 = new Product();
        product1.setId(1L);
        product1.setSku("PHONE-001");
        product1.setName("Samsung Galaxy");
        product1.setPrice(new BigDecimal("50000.00"));
        product1.setCategory("Electronics");

        Product product2 = new Product();
        product2.setId(2L);
        product2.setSku("LAPTOP-001");
        product2.setName("Dell Inspiron");
        product2.setPrice(new BigDecimal("65000.00"));
        product2.setCategory("Electronics");

        when(productRepository.findByCategory("Electronics"))
                .thenReturn(List.of(product1, product2));

        List<ProductResponse> responses =
                productService.getProductsByCategory("Electronics");

        assertEquals(2, responses.size());
        assertEquals("Samsung Galaxy", responses.get(0).getName());
        assertEquals("Dell Inspiron", responses.get(1).getName());

        verify(productRepository).findByCategory("Electronics");
    }


    @Test
    void updateProduct_shouldUpdateSuccessfully_whenProductExists() {
        Product existingProduct = new Product();
        existingProduct.setId(1L);
        existingProduct.setSku("PHONE-001");
        existingProduct.setName("Samsung Galaxy");
        existingProduct.setPrice(new BigDecimal("50000.00"));
        existingProduct.setCategory("Electronics");

        ProductRequest request = new ProductRequest();
        request.setSku("PHONE-001");
        request.setName("Samsung Galaxy S25");
        request.setDescription("Updated description");
        request.setPrice(new BigDecimal("60000.00"));
        request.setCategory("Electronics");
        request.setBrand("Samsung");

        when(productRepository.findById(1L))
                .thenReturn(Optional.of(existingProduct));

        when(productRepository.save(any(Product.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ProductResponse response = productService.updateProduct(1L, request);

        assertNotNull(response);
        assertEquals("Samsung", existingProduct.getBrand());
        assertEquals("Samsung", response.getBrand());
        assertEquals("Samsung Galaxy S25", response.getName());
        assertEquals(new BigDecimal("60000.00"), response.getPrice());

        verify(productRepository).findById(1L);
        verify(productRepository).save(existingProduct);
    }

    @Test
    void updateProduct_shouldThrowException_whenSkuAlreadyExists() {
        Product existingProduct = new Product();
        existingProduct.setId(1L);
        existingProduct.setSku("PHONE-001");

        ProductRequest request = new ProductRequest();
        request.setSku("PHONE-002");
        request.setName("Updated Phone");
        request.setPrice(new BigDecimal("50000.00"));
        request.setCategory("Electronics");

        when(productRepository.findById(1L))
                .thenReturn(Optional.of(existingProduct));

        when(productRepository.existsBySku("PHONE-002"))
                .thenReturn(true);

        assertThrows(DuplicateSkuException.class,
                () -> productService.updateProduct(1L, request));

        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void updateProduct_shouldThrowException_whenProductDoesNotExist() {
        ProductRequest request = new ProductRequest();

        when(productRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThrows(ProductNotFoundException.class,
                () -> productService.updateProduct(99L, request));

        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void deleteProduct_shouldDeleteSuccessfully_whenProductExists() {
        when(productRepository.existsById(1L))
                .thenReturn(true);

        assertDoesNotThrow(() -> productService.deleteProduct(1L));

        verify(productRepository).existsById(1L);
        verify(productRepository).deleteById(1L);
    }

    @Test
    void deleteProduct_shouldThrowException_whenProductDoesNotExist() {
        when(productRepository.existsById(99L))
                .thenReturn(false);

        assertThrows(ProductNotFoundException.class,
                () -> productService.deleteProduct(99L));

        verify(productRepository, never()).deleteById(99L);
    }

}
