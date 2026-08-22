package com.project_aegis.inventory_service.exception;

public class InvalidStateTransitionException extends RuntimeException {

    public InvalidStateTransitionException(String message) {
        super(message);
    }

    public InvalidStateTransitionException(String currentState, String targetState) {
        super(String.format("Cannot transition from %s to %s", currentState, targetState));
    }
}
