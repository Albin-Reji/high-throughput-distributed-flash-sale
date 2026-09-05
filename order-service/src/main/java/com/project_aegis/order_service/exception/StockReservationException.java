package com.project_aegis.order_service.exception;

public class StockReservationException extends RuntimeException {
    public StockReservationException(String message, Throwable cause) {
        super(message, cause);
    }
}
