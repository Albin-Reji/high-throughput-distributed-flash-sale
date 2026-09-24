-- V2__idempotency_unique_constraint.sql
-- Add composite unique constraint to enforce idempotency per customer

ALTER TABLE idempotency_records ADD CONSTRAINT uq_idempotency_key_customer UNIQUE (idempotency_key, customer_id);
