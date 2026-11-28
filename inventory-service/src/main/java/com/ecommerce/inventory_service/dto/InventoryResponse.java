package com.ecommerce.inventory_service.dto;

import com.ecommerce.inventory_service.entity.Inventory;

import java.time.LocalDateTime;

public record InventoryResponse(

        Long id,
        Long productId,
        String sku,
        Integer totalQuantity,
        Integer reservedQuantity,
        Integer availableQuantity,
        LocalDateTime createdAt,
        LocalDateTime updatedAt

) {

    public static InventoryResponse fromEntity(Inventory inventory) {

        return new InventoryResponse(
                inventory.getId(),
                inventory.getProductId(),
                inventory.getSku(),
                inventory.getTotalQuantity(),
                inventory.getReservedQuantity(),
                inventory.getAvailableQuantity(),
                inventory.getCreatedAt(),
                inventory.getUpdatedAt()
        );
    }
}