package com.project_aegis.order_service.exception;

public class UserServiceClientException extends RuntimeException {
    public UserServiceClientException(String message) {
        super(message);
    }

    public UserServiceClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
