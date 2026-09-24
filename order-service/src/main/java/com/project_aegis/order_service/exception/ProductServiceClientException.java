package com.project_aegis.order_service.exception;

public class ProductServiceClientException extends RuntimeException {
    public ProductServiceClientException(String s) {
        super(s);
    }

    public ProductServiceClientException(String s, Exception ex) {
        super(s, ex);
    }
}
