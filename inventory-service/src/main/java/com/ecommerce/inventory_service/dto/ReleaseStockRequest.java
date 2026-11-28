package com.ecommerce.inventory_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReleaseStockRequest(

        @NotBlank(message = "Order ID is required")
        @Size(max = 100, message = "Order ID cannot exceed 100 characters")
        String orderId

) {
}