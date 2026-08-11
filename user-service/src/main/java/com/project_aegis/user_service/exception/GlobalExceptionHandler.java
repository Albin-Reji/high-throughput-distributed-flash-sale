package com.project_aegis.user_service.exception;

import com.project_aegis.user_service.dto.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * Global exception handler that ensures all errors are returned
 * as structured {@link ApiResponse} JSON objects.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

        @ExceptionHandler(CustomerNotFoundException.class)
        public ResponseEntity<ApiResponse<Void>> handleCustomerNotFoundException(CustomerNotFoundException e) {
                log.warn("Customer not found: {}", e.getMessage());
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                .body(ApiResponse.<Void>builder()
                                                .success(false)
                                                .message(e.getMessage())
                                                .build());
        }
        /**
         * Handles validation errors from {@code @Valid} annotated request bodies.
         */
        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<ApiResponse<Void>> handleValidationException(
                        MethodArgumentNotValidException ex) {

                String errors = ex.getBindingResult().getFieldErrors().stream()
                                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                                .collect(Collectors.joining("; "));

                log.warn("Validation failed: {}", errors);

                return ResponseEntity.badRequest()
                                .body(ApiResponse.<Void>builder()
                                                .success(false)
                                                .message("Validation failed: " + errors)
                                                .build());
        }

        /**
         * Handles missing required request headers (e.g., X-Internal-Api-Key).
         */
        @ExceptionHandler(MissingRequestHeaderException.class)
        public ResponseEntity<ApiResponse<Void>> handleMissingHeader(
                        MissingRequestHeaderException ex) {

                log.warn("Missing required header: {}", ex.getHeaderName());

                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                .body(ApiResponse.<Void>builder()
                                                .success(false)
                                                .message("Missing required header: " + ex.getHeaderName())
                                                .build());
        }

        /**
         * Handles database constraint violations (e.g., duplicate keycloakUserId
         * in a race condition).
         */
        @ExceptionHandler(DataIntegrityViolationException.class)
        public ResponseEntity<ApiResponse<Void>> handleDataIntegrityViolation(
                        DataIntegrityViolationException ex) {

                log.warn("Data integrity violation: {}", ex.getMostSpecificCause().getMessage());

                return ResponseEntity.status(HttpStatus.CONFLICT)
                                .body(ApiResponse.<Void>builder()
                                                .success(false)
                                                .message("Data conflict — the resource may already exist")
                                                .build());
        }

        /**
         * Catch-all for unexpected exceptions.
         */
        @ExceptionHandler(Exception.class)
        public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex) {

                log.error("Unexpected error", ex);

                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body(ApiResponse.<Void>builder()
                                                .success(false)
                                                .message("An unexpected error occurred")
                                                .build());
        }
}
