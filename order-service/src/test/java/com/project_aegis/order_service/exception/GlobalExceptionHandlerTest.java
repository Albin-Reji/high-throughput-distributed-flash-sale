package com.project_aegis.order_service.exception;

import com.project_aegis.order_service.dto.response.ApiResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GlobalExceptionHandler Unit Tests")
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("Should return 409 CONFLICT when OptimisticLockingFailureException is handled")
    void shouldHandleOptimisticLockingFailure() {
        OptimisticLockingFailureException ex = new OptimisticLockingFailureException("Row updated by another transaction");

        ResponseEntity<ApiResponse<Void>> response = handler.handleOptimisticLocking(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getSuccess()).isFalse();
        assertThat(response.getBody().getMessage()).contains("updated concurrently");
    }

    @Test
    @DisplayName("Should return 409 CONFLICT when InvalidStateTransitionException is handled")
    void shouldHandleInvalidStateTransition() {
        InvalidStateTransitionException ex = new InvalidStateTransitionException("Invalid transition from CANCELLED to PAID");

        ResponseEntity<ApiResponse<Void>> response = handler.handleInvalidStateTransition(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getSuccess()).isFalse();
        assertThat(response.getBody().getMessage()).contains("Invalid transition");
    }
}
