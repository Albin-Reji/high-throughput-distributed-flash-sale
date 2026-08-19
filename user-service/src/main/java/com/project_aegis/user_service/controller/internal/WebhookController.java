package com.project_aegis.user_service.controller.internal;

import com.project_aegis.user_service.config.InternalApiProperties;
import com.project_aegis.user_service.dto.response.ApiResponse;
import com.project_aegis.user_service.dto.response.ProfileCreationResult;
import com.project_aegis.user_service.dto.webhook.KeycloakUserRegisteredEvent;
import com.project_aegis.user_service.entity.CustomerProfile;
import com.project_aegis.user_service.service.CustomerProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Internal webhook controller for receiving events from Keycloak.
 *
 * <p>This endpoint is <b>not</b> protected by OAuth2/JWT. Instead, it uses
 * a shared API key passed in the {@code X-Internal-Api-Key} header.
 * The endpoint is intended to be called only from the Keycloak SPI
 * running in the same network.</p>
 */
@RestController
@RequestMapping("/internal/webhook/keycloak")
@RequiredArgsConstructor
@Slf4j
public class WebhookController {

    private final CustomerProfileService customerProfileService;
    private final InternalApiProperties internalApiProperties;

    @PostMapping("/user-registered")
    public ResponseEntity<ApiResponse<Void>> handleUserRegistered(
            @RequestHeader("X-Internal-Api-Key") String apiKey,
            @Valid @RequestBody KeycloakUserRegisteredEvent event) {

        // Validate the API key
        String configuredKey = internalApiProperties.getKey();
        log.debug("Configured API key: [{}], Received API key: [{}]", configuredKey, apiKey);

        if (configuredKey == null || !configuredKey.equals(apiKey)) {
            log.warn("Webhook call rejected — invalid API key. Configured key is {}",
                    configuredKey == null ? "NULL" : "present but mismatched");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.<Void>builder()
                            .success(false)
                            .message("Invalid API key")
                            .build());
        }

        log.info("Received user-registered webhook for keycloakUserId={}", event.keycloakUserId());

        ProfileCreationResult profile =
                customerProfileService.createProfileFromKeycloakEvent(event);

//        if profile.created is true then it's new Profile
//        else it's a new Profile
        boolean isNew = profile.isCreated();

        HttpStatus status = isNew ? HttpStatus.CREATED : HttpStatus.OK;
        String message = isNew
                ? "Customer profile created successfully"
                : "Customer profile already exists";

        return ResponseEntity.status(status)
                .body(ApiResponse.<Void>builder()
                        .success(true)
                        .message(message)
                        .build());
    }
}
