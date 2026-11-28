
package com.ecommerce.inventory_service.service;

import com.ecommerce.inventory_service.entity.Inventory;
import com.ecommerce.inventory_service.entity.StockMovement;
import com.ecommerce.inventory_service.entity.StockMovementType;
import com.ecommerce.inventory_service.exception.InventoryNotFoundException;
import com.ecommerce.inventory_service.exception.InsufficientStockException;
import com.ecommerce.inventory_service.repository.InventoryRepository;
import com.ecommerce.inventory_service.repository.StockMovementRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private StockMovementRepository stockMovementRepository;

    @InjectMocks
    private InventoryService inventoryService;

    private Inventory inventory;

    @BeforeEach
    void setUp() {
        inventory = Inventory.builder()
                .id(1L)
                .productId(101L)
                .sku("PHONE-001")
                .totalQuantity(100)
                .reservedQuantity(20)
                .build();
    }

    // 1. Test adding stock
    @Test
    void shouldAddStockSuccessfully() {

        when(inventoryRepository.findByProductId(101L))
                .thenReturn(Optional.of(inventory));

        when(inventoryRepository.save(any(Inventory.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Inventory result = inventoryService.addStock(
                101L, "PHONE-001", 50
        );

        assertEquals(150, result.getTotalQuantity());

        verify(inventoryRepository).save(any(Inventory.class));

        verify(stockMovementRepository).save(
                argThat(movement ->
                        movement.getMovementType() == StockMovementType.STOCK_IN
                                && movement.getQuantity() == 50
                )
        );
    }

    // 2. Test retrieving inventory
    @Test
    void shouldGetInventorySuccessfully() {

        when(inventoryRepository.findByProductId(101L))
                .thenReturn(Optional.of(inventory));

        Inventory result = inventoryService.getInventory(101L);

        assertEquals(101L, result.getProductId());
        assertEquals(100, result.getTotalQuantity());
        assertEquals(80, result.getAvailableQuantity());
    }

    // 3. Test inventory not found
    @Test
    void shouldThrowExceptionWhenInventoryNotFound() {

        when(inventoryRepository.findByProductId(999L))
                .thenReturn(Optional.empty());

        assertThrows(
                InventoryNotFoundException.class,
                () -> inventoryService.getInventory(999L)
        );
    }

    // 4. Test successful reservation
    @Test
    void shouldReserveStockSuccessfully() {

        when(inventoryRepository.findByProductIdForUpdate(101L))
                .thenReturn(Optional.of(inventory));

        when(inventoryRepository.save(any(Inventory.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Inventory result = inventoryService.reserveStock(101L, 10);

        assertEquals(30, result.getReservedQuantity());
        assertEquals(70, result.getAvailableQuantity());

        verify(stockMovementRepository).save(
                argThat(movement ->
                        movement.getMovementType() == StockMovementType.RESERVED
                                && movement.getQuantity() == 10
                )
        );
    }

    // 5. Test insufficient stock
    @Test
    void shouldThrowExceptionWhenStockIsInsufficient() {

        when(inventoryRepository.findByProductIdForUpdate(101L))
                .thenReturn(Optional.of(inventory));

        assertThrows(
                InsufficientStockException.class,
                () -> inventoryService.reserveStock(101L, 100)
        );

        verify(inventoryRepository, never()).save(any(Inventory.class));
        verify(stockMovementRepository, never()).save(any(StockMovement.class));
    }

    // 6. Test successful stock release
    @Test
    void shouldReleaseStockSuccessfully() {

        when(inventoryRepository.findByProductIdForUpdate(101L))
                .thenReturn(Optional.of(inventory));

        when(inventoryRepository.save(any(Inventory.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Inventory result = inventoryService.releaseStock(101L, 10);

        assertEquals(10, result.getReservedQuantity());
        assertEquals(90, result.getAvailableQuantity());

        verify(stockMovementRepository).save(
                argThat(movement ->
                        movement.getMovementType() == StockMovementType.RELEASED
                                && movement.getQuantity() == 10
                )
        );
    }

    // 7. Test invalid release
    @Test
    void shouldThrowExceptionWhenReleaseExceedsReservedStock() {

        when(inventoryRepository.findByProductIdForUpdate(101L))
                .thenReturn(Optional.of(inventory));

        assertThrows(
                InsufficientStockException.class,
                () -> inventoryService.releaseStock(101L, 30)
        );

        verify(inventoryRepository, never()).save(any(Inventory.class));
        verify(stockMovementRepository, never()).save(any(StockMovement.class));
    }
}
