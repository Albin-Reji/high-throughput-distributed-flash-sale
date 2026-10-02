package com.aegis.payment_service.exception;

public class InvalidWebhookSignatureException extends RuntimeException {

    public InvalidWebhookSignatureException(String message) {
        super(message);
    }

    public InvalidWebhookSignatureException(String message, Throwable cause) {
        super(message, cause);
    }
}
