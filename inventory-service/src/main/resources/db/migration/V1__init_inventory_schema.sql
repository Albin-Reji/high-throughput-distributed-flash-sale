-- V1__init_inventory_schema.sql
-- Inventory Service Initial Schema

-- 1. Inventory Table
CREATE TABLE IF NOT EXISTS inventory (
    sku_id UUID PRIMARY KEY,
    total_quantity INT NOT NULL CHECK (total_quantity >= 0),
    available_quantity INT NOT NULL CHECK (available_quantity >= 0),
    version BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_inventory_updated_at ON inventory(updated_at);

-- 2. Flash Campaign Table
CREATE TABLE IF NOT EXISTS flash_campaign (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL UNIQUE,
    start_time TIMESTAMP WITH TIME ZONE NOT NULL,
    end_time TIMESTAMP WITH TIME ZONE NOT NULL,
    status VARCHAR(50) NOT NULL,
    CONSTRAINT chk_campaign_time CHECK (end_time > start_time)
);

CREATE INDEX IF NOT EXISTS idx_flash_campaign_status ON flash_campaign(status);
CREATE INDEX IF NOT EXISTS idx_flash_campaign_dates ON flash_campaign(start_time, end_time);

-- 3. Flash Campaign SKU Table
CREATE TABLE IF NOT EXISTS flash_campaign_sku (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    campaign_id UUID NOT NULL REFERENCES flash_campaign(id) ON DELETE CASCADE,
    sku_id UUID NOT NULL,
    flash_price NUMERIC(19, 2) NOT NULL CHECK (flash_price > 0),
    allocated_stock INT NOT NULL CHECK (allocated_stock > 0),
    max_per_user INT NOT NULL DEFAULT 10 CHECK (max_per_user > 0),
    CONSTRAINT uk_campaign_sku UNIQUE (campaign_id, sku_id)
);

CREATE INDEX IF NOT EXISTS idx_campaign_sku_campaign ON flash_campaign_sku(campaign_id);
CREATE INDEX IF NOT EXISTS idx_campaign_sku_sku ON flash_campaign_sku(sku_id);

-- 4. Stock Reservation Table
CREATE TABLE IF NOT EXISTS stock_reservation (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL,
    customer_id UUID,
    campaign_id UUID REFERENCES flash_campaign(id) ON DELETE SET NULL,
    sku_id UUID NOT NULL,
    quantity INT NOT NULL CHECK (quantity > 0),
    status VARCHAR(50) NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_reservation_order_id ON stock_reservation(order_id);
CREATE INDEX IF NOT EXISTS idx_reservation_sku_id ON stock_reservation(sku_id);
CREATE INDEX IF NOT EXISTS idx_reservation_status_expires ON stock_reservation(status, expires_at);
