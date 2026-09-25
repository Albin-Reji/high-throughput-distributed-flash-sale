package com.project_aegis.inventory_service.exception;

import com.project_aegis.inventory_service.dto.response.ApiResponse;
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

        ResponseEntity<ApiResponse<Void>> response = handler.handleMaxUploadSizeExceeded(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getSuccess()).isFalse();
        assertThat(response.getBody().getMessage()).contains("Payload too large");
    }

    @Test
    @DisplayName("Should return 404 NOT_FOUND when ResourceNotFoundException is handled")
    void shouldHandleResourceNotFound() {
        ResourceNotFoundException ex = new ResourceNotFoundException("SKU not found");

        ResponseEntity<ApiResponse<Void>> response = handler.handleResourceNotFound(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getSuccess()).isFalse();
        assertThat(response.getBody().getMessage()).isEqualTo("SKU not found");
    }
}
