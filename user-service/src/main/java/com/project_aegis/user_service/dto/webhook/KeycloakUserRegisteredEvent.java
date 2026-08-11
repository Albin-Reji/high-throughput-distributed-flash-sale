package com.project_aegis.user_service.dto.webhook;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Payload sent by the Keycloak Event Listener SPI when a user registers.
 *
 * <p>This record is intentionally minimal — it contains only the fields
 * that Keycloak provides at registration time. Additional profile data
 * (phone, address, preferences) is populated later by the user.</p>
 */
public record KeycloakUserRegisteredEvent(

        @NotBlank(message = "keycloakUserId is required")
        @Size(max = 36, message = "keycloakUserId must not exceed 36 characters")
        String keycloakUserId,

        @NotBlank(message = "email is required")
        @Email(message = "email must be a valid email address")
        @Size(max = 255, message = "email must not exceed 255 characters")
        String email,

        @NotBlank(message = "firstName is required")
        @Size(max = 100, message = "firstName must not exceed 100 characters")
        String firstName,

        @NotBlank(message = "lastName is required")
        @Size(max = 100, message = "lastName must not exceed 100 characters")
        String lastName
) {
}
