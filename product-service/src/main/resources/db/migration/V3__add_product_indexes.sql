
-- Index for product category searches
CREATE INDEX idx_products_category
ON products(category);

-- Partial index for active products
CREATE INDEX idx_products_active_true
ON products(active)
WHERE active = TRUE;
