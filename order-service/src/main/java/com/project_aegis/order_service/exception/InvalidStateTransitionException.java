package com.project_aegis.order_service.exception;

import com.project_aegis.order_service.entity.OrderStatus;

public class InvalidStateTransitionException extends RuntimeException {

    public InvalidStateTransitionException(String message) {
        super(message);
    }

    public InvalidStateTransitionException(OrderStatus current, OrderStatus target) {
        super(String.format("Invalid order state transition from %s to %s", current, target));
    }
}

