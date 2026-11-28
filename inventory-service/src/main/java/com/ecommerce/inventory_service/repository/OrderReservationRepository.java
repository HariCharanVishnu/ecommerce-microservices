package com.ecommerce.inventory_service.repository;

import com.ecommerce.inventory_service.entity.OrderReservation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrderReservationRepository
        extends JpaRepository<OrderReservation, Long> {

    Optional<OrderReservation> findByOrderIdAndInventoryId(
            String orderId,
            Long inventoryId
    );
}