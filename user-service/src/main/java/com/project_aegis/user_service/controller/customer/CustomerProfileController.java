package com.project_aegis.user_service.controller.customer;

import com.project_aegis.user_service.dto.customer.request.CustomerUpdateRequest;
import com.project_aegis.user_service.dto.customer.response.CustomerProfileResponse;
import com.project_aegis.user_service.dto.response.ApiResponse;
import com.project_aegis.user_service.service.CustomerProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/customers/me")
public class CustomerProfileController {

    private final CustomerProfileService customerProfileService;

    /**
     * Get current authenticated customer profile.
     *
     * @param jwt authenticated JWT token
     * @return {@link ApiResponse} containing {@link CustomerProfileResponse}
     */
    @GetMapping
    public ResponseEntity<ApiResponse<CustomerProfileResponse>> getCurrentUser(@AuthenticationPrincipal Jwt jwt) {
        String keycloakUserId = jwt.getSubject();
        return ResponseEntity.ok(customerProfileService.getCurrentCustomer(keycloakUserId));
    }

    /**
     * Modify existing customer profile (partial update).
     *
     * @param jwt                   authenticated JWT token
     * @param customerUpdateRequest update payload
     * @return {@link ApiResponse} containing {@link CustomerProfileResponse}
     */
    @PatchMapping
    public ResponseEntity<ApiResponse<CustomerProfileResponse>> modifyCurrentUser(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody CustomerUpdateRequest customerUpdateRequest) {
        String keycloakUserId = jwt.getSubject();
        return ResponseEntity.ok(customerProfileService.modifyCurrentUser(keycloakUserId, customerUpdateRequest));
    }

    /**
     * Update existing customer profile (full update).
     *
     * @param jwt                   authenticated JWT token
     * @param customerUpdateRequest update payload
     * @return {@link ApiResponse} containing {@link CustomerProfileResponse}
     */
    @PutMapping
    public ResponseEntity<ApiResponse<CustomerProfileResponse>> updateCurrentUser(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody CustomerUpdateRequest customerUpdateRequest) {
        String keycloakUserId = jwt.getSubject();
        return ResponseEntity.ok(customerProfileService.updateCurrentUser(keycloakUserId, customerUpdateRequest));
    }
}
