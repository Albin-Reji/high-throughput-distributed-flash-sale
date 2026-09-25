package com.aegis.product_service.exception;

import com.aegis.product_service.dto.common.ErrorResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GlobalExceptionHandler Unit Tests")
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("Should return 413 PAYLOAD_TOO_LARGE when MaxUploadSizeExceededException is handled")
    void shouldHandleMaxUploadSizeExceeded() {
        MaxUploadSizeExceededException ex = new MaxUploadSizeExceededException(2097152);

        ResponseEntity<ErrorResponse> response = handler.handleMaxUploadSizeExceeded(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(413);
        assertThat(response.getBody().getMessage()).contains("Payload too large");
    }

    @Test
    @DisplayName("Should return 404 NOT_FOUND when ResourceNotFound is handled")
    void shouldHandleResourceNotFound() {
        ResourceNotFound ex = new ResourceNotFound("Product not found");

        ResponseEntity<ErrorResponse> response = handler.handleNotFound(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(404);
        assertThat(response.getBody().getMessage()).isEqualTo("Product not found");
    }
}
