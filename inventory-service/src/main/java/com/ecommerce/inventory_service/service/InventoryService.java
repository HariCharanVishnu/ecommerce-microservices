
package com.ecommerce.inventory_service.service;

import com.ecommerce.inventory_service.entity.Inventory;
import com.ecommerce.inventory_service.entity.OrderReservation;
import com.ecommerce.inventory_service.entity.ReservationStatus;
import com.ecommerce.inventory_service.entity.StockMovement;
import com.ecommerce.inventory_service.entity.StockMovementType;

import com.ecommerce.inventory_service.exception.InventoryNotFoundException;
import com.ecommerce.inventory_service.exception.InsufficientStockException;

import com.ecommerce.inventory_service.repository.InventoryRepository;
import com.ecommerce.inventory_service.repository.OrderReservationRepository;
import com.ecommerce.inventory_service.repository.StockMovementRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final StockMovementRepository stockMovementRepository;
    private final OrderReservationRepository orderReservationRepository;

    // 1. Add stock
    @Transactional
    public Inventory addStock(Long productId, String sku, Integer quantity) {

        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException(
                    "Stock quantity must be greater than zero"
            );
        }

        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseGet(() -> Inventory.builder()
                        .productId(productId)
                        .sku(sku)
                        .totalQuantity(0)
                        .reservedQuantity(0)
                        .build());

        if (!inventory.getSku().equals(sku)) {
            throw new IllegalArgumentException(
                    "SKU does not match the existing inventory record"
            );
        }

        inventory.setTotalQuantity(
                inventory.getTotalQuantity() + quantity
        );

        Inventory savedInventory = inventoryRepository.save(inventory);

        StockMovement movement = StockMovement.builder()
                .productId(productId)
                .movementType(StockMovementType.STOCK_IN)
                .quantity(quantity)
                .build();

        stockMovementRepository.save(movement);

        return savedInventory;
    }

    // 2. Get inventory
    @Transactional(readOnly = true)
    public Inventory getInventory(Long productId) {

        return inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new InventoryNotFoundException(
                        "Inventory not found for product ID: " + productId
                ));
    }

    // 3. Original reserve method (temporary compatibility)
    @Transactional
    public Inventory reserveStock(Long productId, Integer quantity) {

        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException(
                    "Reservation quantity must be greater than zero"
            );
        }

        Inventory inventory = inventoryRepository
                .findByProductIdForUpdate(productId)
                .orElseThrow(() -> new InventoryNotFoundException(
                        "Inventory not found for product ID: " + productId
                ));

        if (inventory.getAvailableQuantity() < quantity) {
            throw new InsufficientStockException(
                    "Insufficient stock available"
            );
        }

        inventory.setReservedQuantity(
                inventory.getReservedQuantity() + quantity
        );

        Inventory savedInventory = inventoryRepository.save(inventory);

        StockMovement movement = StockMovement.builder()
                .productId(productId)
                .movementType(StockMovementType.RESERVED)
                .quantity(quantity)
                .build();

        stockMovementRepository.save(movement);

        return savedInventory;
    }

    // 4. Original release method (temporary compatibility)
    @Transactional
    public Inventory releaseStock(Long productId, Integer quantity) {

        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException(
                    "Release quantity must be greater than zero"
            );
        }

        Inventory inventory = inventoryRepository
                .findByProductIdForUpdate(productId)
                .orElseThrow(() -> new InventoryNotFoundException(
                        "Inventory not found for product ID: " + productId
                ));

        if (inventory.getReservedQuantity() < quantity) {
            throw new InsufficientStockException(
                    "Cannot release more stock than reserved"
            );
        }

        inventory.setReservedQuantity(
                inventory.getReservedQuantity() - quantity
        );

        Inventory savedInventory = inventoryRepository.save(inventory);

        StockMovement movement = StockMovement.builder()
                .productId(productId)
                .movementType(StockMovementType.RELEASED)
                .quantity(quantity)
                .build();

        stockMovementRepository.save(movement);

        return savedInventory;
    }

    // 5. Reserve stock for a specific order
    @Transactional
    public Inventory reserveStockForOrder(
            String orderId,
            Long productId,
            Integer quantity
    ) {

        if (orderId == null || orderId.isBlank()) {
            throw new IllegalArgumentException("Order ID cannot be empty");
        }

        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException(
                    "Reservation quantity must be greater than zero"
            );
        }

        // Lock inventory row to prevent concurrent overselling
        Inventory inventory = inventoryRepository
                .findByProductIdForUpdate(productId)
                .orElseThrow(() -> new InventoryNotFoundException(
                        "Inventory not found for product ID: " + productId
                ));

        // Check for an existing reservation
        var existingReservation = orderReservationRepository
                .findByOrderIdAndInventoryId(orderId, inventory.getId());

        if (existingReservation.isPresent()) {

            OrderReservation reservation = existingReservation.get();

            if (reservation.getStatus() == ReservationStatus.RELEASED) {
                throw new IllegalStateException(
                        "This order's reservation was already released"
                );
            }

            if (!reservation.getQuantity().equals(quantity)) {
                throw new IllegalStateException(
                        "This order already has a reservation with a different quantity"
                );
            }

            // Duplicate request: do not reserve stock again
            return inventory;
        }

        if (inventory.getAvailableQuantity() < quantity) {
            throw new InsufficientStockException(
                    "Insufficient stock available for product ID: " + productId
            );
        }

        inventory.setReservedQuantity(
                inventory.getReservedQuantity() + quantity
        );

        Inventory savedInventory = inventoryRepository.save(inventory);

        // Create order reservation record
        OrderReservation reservation = OrderReservation.builder()
                .orderId(orderId)
                .inventory(inventory)
                .quantity(quantity)
                .status(ReservationStatus.RESERVED)
                .build();

        orderReservationRepository.save(reservation);

        // Record stock movement
        StockMovement movement = StockMovement.builder()
                .productId(productId)
                .movementType(StockMovementType.RESERVED)
                .quantity(quantity)
                .referenceId(orderId)
                .build();

        stockMovementRepository.save(movement);

        return savedInventory;
    }

    // 6. Release stock for a specific order
    @Transactional
    public Inventory releaseStockForOrder(
            String orderId,
            Long productId
    ) {

        if (orderId == null || orderId.isBlank()) {
            throw new IllegalArgumentException("Order ID cannot be empty");
        }

        Inventory inventory = inventoryRepository
                .findByProductIdForUpdate(productId)
                .orElseThrow(() -> new InventoryNotFoundException(
                        "Inventory not found for product ID: " + productId
                ));

        // Find the reservation belonging to this order and product
        OrderReservation reservation = orderReservationRepository
                .findByOrderIdAndInventoryId(orderId, inventory.getId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Reservation not found for order: " + orderId
                ));

        // Repeated release request: do not release twice
        if (reservation.getStatus() == ReservationStatus.RELEASED) {
            return inventory;
        }

        inventory.setReservedQuantity(
                inventory.getReservedQuantity() - reservation.getQuantity()
        );

        reservation.setStatus(ReservationStatus.RELEASED);

        Inventory savedInventory = inventoryRepository.save(inventory);

        orderReservationRepository.save(reservation);

        // Record stock movement
        StockMovement movement = StockMovement.builder()
                .productId(productId)
                .movementType(StockMovementType.RELEASED)
                .quantity(reservation.getQuantity())
                .referenceId(orderId)
                .build();

        stockMovementRepository.save(movement);

        return savedInventory;
    }

    // 7. Confirm stock after successful payment
    @Transactional
    public Inventory confirmStockForOrder(
            String orderId,
            Long productId
    ) {

        if (orderId == null || orderId.isBlank()) {
            throw new IllegalArgumentException("Order ID cannot be empty");
        }

        // Lock inventory to prevent concurrent stock modifications
        Inventory inventory = inventoryRepository
                .findByProductIdForUpdate(productId)
                .orElseThrow(() -> new InventoryNotFoundException(
                        "Inventory not found for product ID: " + productId
                ));

        // Find reservation belonging to this order and product
        OrderReservation reservation = orderReservationRepository
                .findByOrderIdAndInventoryId(orderId, inventory.getId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Reservation not found for order: " + orderId
                ));

        // Prevent confirming a released reservation
        if (reservation.getStatus() == ReservationStatus.RELEASED) {
            throw new IllegalStateException(
                    "Cannot confirm a released reservation"
            );
        }

        // Duplicate confirmation: do not deduct stock again
        if (reservation.getStatus() == ReservationStatus.CONFIRMED) {
            return inventory;
        }

        Integer quantity = reservation.getQuantity();

        // Deduct confirmed quantity from total and reserved stock
        inventory.setTotalQuantity(
                inventory.getTotalQuantity() - quantity
        );

        inventory.setReservedQuantity(
                inventory.getReservedQuantity() - quantity
        );

        // Update reservation status
        reservation.setStatus(ReservationStatus.CONFIRMED);

        Inventory savedInventory = inventoryRepository.save(inventory);

        orderReservationRepository.save(reservation);

        // Record stock movement
        StockMovement movement = StockMovement.builder()
                .productId(productId)
                .movementType(StockMovementType.STOCK_OUT)
                .quantity(quantity)
                .referenceId(orderId)
                .build();

        stockMovementRepository.save(movement);

        return savedInventory;
    }
}
