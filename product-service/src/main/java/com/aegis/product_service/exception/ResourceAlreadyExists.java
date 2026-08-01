package com.aegis.product_service.exception;

public class ResourceAlreadyExists extends RuntimeException {
    public ResourceAlreadyExists(String s) {
        super(s);
    }
}
