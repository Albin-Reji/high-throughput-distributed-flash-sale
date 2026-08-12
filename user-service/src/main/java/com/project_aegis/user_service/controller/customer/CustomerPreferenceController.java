package com.project_aegis.user_service.controller.customer;

import com.project_aegis.user_service.dto.customer.request.CustomerPreferenceRequest;
import com.project_aegis.user_service.dto.customer.response.CustomerPreferenceResponse;
import com.project_aegis.user_service.service.CustomerPreferenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for managing customer preferences (/api/v1/customers/me/preferences).
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/customers/me/preferences")
public class CustomerPreferenceController {

    private final CustomerPreferenceService customerPreferenceService;

    /**
     * Get preferences for the authenticated customer.
     */
    @GetMapping
    public ResponseEntity<CustomerPreferenceResponse> getPreferences(
            @AuthenticationPrincipal Jwt jwt
    ) {
        String keycloakUserId = jwt.getSubject();
        return ResponseEntity.ok(customerPreferenceService.getPreferences(keycloakUserId));
    }

    /**
     * Update preferences (Full Replace).
     */
    @PutMapping
    public ResponseEntity<CustomerPreferenceResponse> replacePreferences(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody CustomerPreferenceRequest preferenceRequest
    ) {
        String keycloakUserId = jwt.getSubject();
        return ResponseEntity.ok(customerPreferenceService.replacePreferences(keycloakUserId, preferenceRequest));
    }

    /**
     * Partial update preferences.
     */
    @PatchMapping
    public ResponseEntity<CustomerPreferenceResponse> updatePreferences(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody CustomerPreferenceRequest preferenceRequest
    ) {
        String keycloakUserId = jwt.getSubject();
        return ResponseEntity.ok(customerPreferenceService.updatePreferences(keycloakUserId, preferenceRequest));
    }
}
