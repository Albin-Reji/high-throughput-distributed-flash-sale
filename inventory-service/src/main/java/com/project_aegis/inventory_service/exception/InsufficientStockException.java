package com.project_aegis.inventory_service.exception;

public class InsufficientStockException extends RuntimeException {

    public InsufficientStockException(String message) {
        super(message);
    }

    public InsufficientStockException(String skuId, int requested, int available) {
        super(String.format("Insufficient stock for SKU '%s': requested adjustment %d but only %d available",
                skuId, requested, available));
    }
}
