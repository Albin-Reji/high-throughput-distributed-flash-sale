-- V3__add_version_to_orders.sql
-- Add optimistic locking version column to orders table

ALTER TABLE orders ADD COLUMN version INT NOT NULL DEFAULT 0;
