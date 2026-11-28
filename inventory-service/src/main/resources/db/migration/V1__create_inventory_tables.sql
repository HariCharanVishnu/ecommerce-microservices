CREATE TABLE inventory (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL UNIQUE,
    sku VARCHAR(100) NOT NULL UNIQUE,
    total_quantity INTEGER NOT NULL DEFAULT 0,
    reserved_quantity INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_total_quantity
        CHECK (total_quantity >= 0),

    CONSTRAINT chk_reserved_quantity
        CHECK (reserved_quantity >= 0),

    CONSTRAINT chk_reserved_not_exceed_total
        CHECK (reserved_quantity <= total_quantity)
);

CREATE TABLE stock_movements (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL,
    movement_type VARCHAR(30) NOT NULL,
    quantity INTEGER NOT NULL,
    reference_id VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_movement_quantity
        CHECK (quantity > 0),

    CONSTRAINT chk_movement_type
        CHECK (
            movement_type IN (
                'STOCK_IN',
                'STOCK_OUT',
                'RESERVED',
                'RELEASED'
            )
        )
);

CREATE INDEX idx_inventory_product_id
    ON inventory(product_id);

CREATE INDEX idx_stock_movements_product_id
    ON stock_movements(product_id);

CREATE INDEX idx_stock_movements_created_at
    ON stock_movements(created_at);