
package com.ecommerce.inventory_service.controller;

import com.ecommerce.inventory_service.dto.AddStockRequest;
import com.ecommerce.inventory_service.dto.InventoryResponse;
import com.ecommerce.inventory_service.dto.ReserveStockRequest;
import com.ecommerce.inventory_service.dto.ReleaseStockRequest;
import com.ecommerce.inventory_service.service.InventoryService;
import com.ecommerce.inventory_service.dto.ConfirmStockRequest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    // 1. Add stock
    @PostMapping("/add")
    public ResponseEntity<InventoryResponse> addStock(
            @Valid @RequestBody AddStockRequest request
    ) {
        var inventory = inventoryService.addStock(
                request.productId(),
                request.sku(),
                request.quantity()
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(InventoryResponse.fromEntity(inventory));
    }

    // 2. Get inventory
    @GetMapping("/{productId}")
    public ResponseEntity<InventoryResponse> getInventory(
            @PathVariable Long productId
    ) {
        var inventory = inventoryService.getInventory(productId);

        return ResponseEntity.ok(
                InventoryResponse.fromEntity(inventory)
        );
    }

    // 3. Reserve stock for an order
    @PostMapping("/{productId}/reserve")
    public ResponseEntity<InventoryResponse> reserveStock(
            @PathVariable Long productId,
            @Valid @RequestBody ReserveStockRequest request
    ) {
        var inventory = inventoryService.reserveStockForOrder(
                request.orderId(),
                productId,
                request.quantity()
        );

        return ResponseEntity.ok(
                InventoryResponse.fromEntity(inventory)
        );
    }

    // 4. Release stock for an order
    @PostMapping("/{productId}/release")
    public ResponseEntity<InventoryResponse> releaseStock(
            @PathVariable Long productId,
            @Valid @RequestBody ReleaseStockRequest request
    ) {
        var inventory = inventoryService.releaseStockForOrder(
                request.orderId(),
                productId
        );

        return ResponseEntity.ok(
                InventoryResponse.fromEntity(inventory)
        );
    }

    // 5. Confirm stock after successful payment
    @PostMapping("/{productId}/confirm")
    public ResponseEntity<InventoryResponse> confirmStock(
            @PathVariable Long productId,
            @Valid @RequestBody ConfirmStockRequest request
    ) {
        var inventory = inventoryService.confirmStockForOrder(
                request.orderId(),
                productId
        );

        return ResponseEntity.ok(
                InventoryResponse.fromEntity(inventory)
        );
    }
}
