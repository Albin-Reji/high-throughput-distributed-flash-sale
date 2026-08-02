package com.aegis.product_service.exception;

import com.aegis.product_service.dto.common.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    /**
     * <p>Handles validation exceptions.</p>
     *
     * @param ex the validation exception
     * @return {@link ErrorResponse}
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex
    ) {
        String errorMessage = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .findFirst()
                .orElse(ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .message(errorMessage)
                .status(HttpStatus.BAD_REQUEST.value())
                .timestamp(LocalDateTime.now())
                .build();

        log.warn(
                "Validation failed: {}",
                errorMessage
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(errorResponse);
    }

    /**
     * <p>Handles resource not found exceptions.</p>
     *
     * @param ex the resource not found exception
     * @return {@link ErrorResponse}
     */
    @ExceptionHandler(ResourceNotFound.class)
    public ResponseEntity<ErrorResponse> handleNotFound(
            ResourceNotFound ex
    ) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .message(ex.getMessage())
                .status(HttpStatus.NOT_FOUND.value())
                .timestamp(LocalDateTime.now())
                .build();

        log.warn(
                "Resource not found: {}",
                ex.getMessage()
        );

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(errorResponse);
    }

    /**
     * <p>Handles resource already exists exceptions.</p>
     *
     * @param ex the resource already exists exception
     * @return {@link ErrorResponse}
     */
    @ExceptionHandler(ResourceAlreadyExists.class)
    public ResponseEntity<ErrorResponse> handleAlreadyExists(
            ResourceAlreadyExists ex
    ) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .message(ex.getMessage())
                .status(HttpStatus.CONFLICT.value())
                .timestamp(LocalDateTime.now())
                .build();

        log.warn(
                "Resource already exist: {}",
                ex.getMessage()
        );
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(errorResponse);
    }

    /**
     * <p>Handles data integrity violation exceptions.</p>
     *
     * @param ex the data integrity violation exception
     * @return {@link ErrorResponse}
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(
            DataIntegrityViolationException ex
    ) {
        ErrorResponse response = ErrorResponse.builder()
                .message("Database error: " + ex.getMostSpecificCause().getMessage())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .timestamp(LocalDateTime.now())
                .build();
        log.warn(
                "Database error: {}",
                ex.getMostSpecificCause().getMessage()
        );
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(response);
    }
}
