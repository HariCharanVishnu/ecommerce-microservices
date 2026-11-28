ALTER TABLE order_reservations
DROP CONSTRAINT chk_reservation_status;

ALTER TABLE order_reservations
ADD CONSTRAINT chk_reservation_status
CHECK (status IN ('RESERVED', 'RELEASED', 'CONFIRMED'));