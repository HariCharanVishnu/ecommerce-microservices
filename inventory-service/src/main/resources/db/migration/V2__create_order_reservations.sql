
CREATE TABLE order_reservations (

    id BIGSERIAL PRIMARY KEY,

    order_id VARCHAR(100) NOT NULL,

    inventory_id BIGINT NOT NULL,

    quantity INTEGER NOT NULL,

    status VARCHAR(20) NOT NULL DEFAULT 'RESERVED',

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_reservation_inventory
        FOREIGN KEY (inventory_id)
        REFERENCES inventory(id),

    CONSTRAINT chk_reservation_quantity
        CHECK (quantity > 0),

    CONSTRAINT chk_reservation_status
        CHECK (status IN ('RESERVED', 'RELEASED')),

    CONSTRAINT uq_order_inventory
        UNIQUE (order_id, inventory_id)
);

CREATE INDEX idx_reservations_order_id
    ON order_reservations(order_id);

CREATE INDEX idx_reservations_inventory_id
    ON order_reservations(inventory_id);
