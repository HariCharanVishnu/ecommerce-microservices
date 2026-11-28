
package com.ecommerce.inventory_service.controller;

import com.ecommerce.inventory_service.entity.Inventory;
import com.ecommerce.inventory_service.exception.GlobalExceptionHandler;
import com.ecommerce.inventory_service.exception.InventoryNotFoundException;
import com.ecommerce.inventory_service.exception.InsufficientStockException;
import com.ecommerce.inventory_service.service.InventoryService;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(InventoryController.class)
@Import(GlobalExceptionHandler.class)
class InventoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InventoryService inventoryService;

    private Inventory createInventory() {
        return Inventory.builder()
                .id(1L)
                .productId(101L)
                .sku("PHONE-001")
                .totalQuantity(100)
                .reservedQuantity(20)
                .build();
    }

    // 1. Test GET inventory
    @Test
    void shouldGetInventorySuccessfully() throws Exception {

        when(inventoryService.getInventory(101L))
                .thenReturn(createInventory());

        mockMvc.perform(get("/api/inventory/101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(101))
                .andExpect(jsonPath("$.totalQuantity").value(100))
                .andExpect(jsonPath("$.reservedQuantity").value(20))
                .andExpect(jsonPath("$.availableQuantity").value(80));
    }

    // 2. Test ADD stock
    @Test
    void shouldAddStockSuccessfully() throws Exception {

        when(inventoryService.addStock(101L, "PHONE-001", 100))
                .thenReturn(createInventory());

        String request = """
                {
                    "productId": 101,
                    "sku": "PHONE-001",
                    "quantity": 100
                }
                """;

        mockMvc.perform(post("/api/inventory/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.productId").value(101))
                .andExpect(jsonPath("$.sku").value("PHONE-001"));
    }

    // 3. Test RESERVE stock for an order
    @Test
    void shouldReserveStockSuccessfully() throws Exception {

        when(inventoryService.reserveStockForOrder(
                "ORD-1001", 101L, 10
        )).thenReturn(createInventory());

        String request = """
                {
                    "orderId": "ORD-1001",
                    "quantity": 10
                }
                """;

        mockMvc.perform(post("/api/inventory/101/reserve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(101));

        verify(inventoryService).reserveStockForOrder(
                "ORD-1001", 101L, 10
        );
    }

    // 4. Test RELEASE stock for an order
    @Test
    void shouldReleaseStockSuccessfully() throws Exception {

        when(inventoryService.releaseStockForOrder(
                "ORD-1001", 101L
        )).thenReturn(createInventory());

        String request = """
                {
                    "orderId": "ORD-1001"
                }
                """;

        mockMvc.perform(post("/api/inventory/101/release")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(101));

        verify(inventoryService).releaseStockForOrder(
                "ORD-1001", 101L
        );
    }

    // 5. Test validation
    @Test
    void shouldRejectNegativeQuantity() throws Exception {

        String request = """
                {
                    "productId": 101,
                    "sku": "PHONE-001",
                    "quantity": -10
                }
                """;

        mockMvc.perform(post("/api/inventory/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(inventoryService);
    }

    // 6. Test inventory not found
    @Test
    void shouldReturn404WhenInventoryNotFound() throws Exception {

        when(inventoryService.getInventory(999L))
                .thenThrow(new InventoryNotFoundException(
                        "Inventory not found for product ID: 999"
                ));

        mockMvc.perform(get("/api/inventory/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    // 7. Test insufficient stock
    @Test
    void shouldReturn409WhenStockIsInsufficient() throws Exception {

        when(inventoryService.reserveStockForOrder(
                "ORD-1002", 101L, 500
        )).thenThrow(new InsufficientStockException(
                "Insufficient stock available"
        ));

        String request = """
                {
                    "orderId": "ORD-1002",
                    "quantity": 500
                }
                """;

        mockMvc.perform(post("/api/inventory/101/reserve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));

        verify(inventoryService).reserveStockForOrder(
                "ORD-1002", 101L, 500
        );
    }

    // 8. Test CONFIRM stock after successful payment
    @Test
    void shouldConfirmStockSuccessfully() throws Exception {

        when(inventoryService.confirmStockForOrder(
                "ORD-1001", 101L
        )).thenReturn(createInventory());

        String request = """
            {
                "orderId": "ORD-1001"
            }
            """;

        mockMvc.perform(post("/api/inventory/101/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(101));

        verify(inventoryService).confirmStockForOrder(
                "ORD-1001", 101L
        );
    }

    // 9. Test confirmation request validation
    @Test
    void shouldRejectConfirmationWithoutOrderId() throws Exception {

        String request = """
            {
                "orderId": ""
            }
            """;

        mockMvc.perform(post("/api/inventory/101/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(inventoryService);
    }


}
