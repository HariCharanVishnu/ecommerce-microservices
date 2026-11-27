
package com.ecommerce.product_service.service;

import com.ecommerce.product_service.dto.ProductMapper;
import com.ecommerce.product_service.dto.ProductRequest;
import com.ecommerce.product_service.dto.ProductResponse;
import com.ecommerce.product_service.entity.Product;
import com.ecommerce.product_service.exception.DuplicateSkuException;
import com.ecommerce.product_service.exception.ProductNotFoundException;
import com.ecommerce.product_service.repository.ProductRepository;

import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@Service
public class ProductService {

    private static final Logger log =
            LoggerFactory.getLogger(ProductService.class);

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    // Create product
    @Transactional
    public ProductResponse saveProduct(ProductRequest request) {

        log.info("Creating product with SKU: {}", request.getSku());

        if (productRepository.existsBySku(request.getSku())) {
            log.warn("Duplicate SKU detected: {}", request.getSku());

            throw new DuplicateSkuException(
                    "Product already exists with SKU: " + request.getSku()
            );
        }

        Product product = ProductMapper.toEntity(request);

        Product savedProduct = productRepository.save(product);

        log.info("Product created successfully. ID: {}, SKU: {}",
                savedProduct.getId(), savedProduct.getSku());

        return ProductMapper.toResponse(savedProduct);
    }

    // Get all products
    @Transactional(readOnly = true)
    public List<ProductResponse> getAllProducts() {

        log.debug("Fetching all products");

        return productRepository.findAll()
                .stream()
                .map(ProductMapper::toResponse)
                .toList();
    }

    // Get product by ID
    @Transactional(readOnly = true)
    public ProductResponse getProductById(Long id) {

        log.info("Fetching product with ID: {}", id);

        Product product = productRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Product not found with ID: {}", id);

                    return new ProductNotFoundException(
                            "Product not found with ID: " + id
                    );
                });

        return ProductMapper.toResponse(product);
    }

    // Get product by SKU
    @Transactional(readOnly = true)
    public ProductResponse getProductBySku(String sku) {

        log.info("Fetching product with SKU: {}", sku);

        Product product = productRepository.findBySku(sku)
                .orElseThrow(() -> {
                    log.warn("Product not found with SKU: {}", sku);

                    return new ProductNotFoundException(
                            "Product not found with SKU: " + sku
                    );
                });

        return ProductMapper.toResponse(product);
    }

    // Get products by category
    @Transactional(readOnly = true)
    public List<ProductResponse> getProductsByCategory(String category) {

        log.debug("Fetching products by category: {}", category);

        return productRepository.findByCategory(category)
                .stream()
                .map(ProductMapper::toResponse)
                .toList();
    }

    // Get active products
    @Transactional(readOnly = true)
    public List<ProductResponse> getActiveProducts() {

        log.debug("Fetching active products");

        return productRepository.findByActiveTrue()
                .stream()
                .map(ProductMapper::toResponse)
                .toList();
    }

    // Delete product
    @Transactional
    public void deleteProduct(Long id) {

        log.info("Deleting product with ID: {}", id);

        if (!productRepository.existsById(id)) {
            log.warn("Cannot delete. Product not found with ID: {}", id);

            throw new ProductNotFoundException(
                    "Product not found with ID: " + id
            );
        }

        productRepository.deleteById(id);

        log.info("Product deleted successfully. ID: {}", id);
    }

    // Update product
    @Transactional
    public ProductResponse updateProduct(Long id, ProductRequest request) {

        log.info("Updating product with ID: {}", id);

        Product product = productRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Cannot update. Product not found with ID: {}", id);

                    return new ProductNotFoundException(
                            "Product not found with ID: " + id
                    );
                });

        if (!product.getSku().equals(request.getSku())
                && productRepository.existsBySku(request.getSku())) {

            log.warn("Duplicate SKU detected during update: {}", request.getSku());

            throw new DuplicateSkuException(
                    "Product already exists with SKU: " + request.getSku()
            );
        }

        product.setSku(request.getSku());
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setCategory(request.getCategory());
        product.setBrand(request.getBrand());

        Product updatedProduct = productRepository.save(product);

        log.info("Product updated successfully. ID: {}", updatedProduct.getId());

        return ProductMapper.toResponse(updatedProduct);
    }

    // Get all products with pagination and sorting
    @Transactional(readOnly = true)
    public Page<ProductResponse> getAllProducts(
            int page, int size, String sortBy, String direction) {

        log.debug("Fetching products. Page: {}, Size: {}, Sort: {}, Direction: {}",
                page, size, sortBy, direction);

        Sort sort = direction.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);

        return productRepository.findAll(pageable)
                .map(ProductMapper::toResponse);
    }
}
