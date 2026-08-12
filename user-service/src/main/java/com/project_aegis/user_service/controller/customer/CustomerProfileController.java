package com.project_aegis.user_service.controller.customer;

import com.project_aegis.user_service.dto.address.response.CustomerAddressResponse;
import com.project_aegis.user_service.dto.address.response.DefaultAddressResponse;
import com.project_aegis.user_service.dto.customer.request.CustomerAddressRequest;
import com.project_aegis.user_service.dto.customer.request.CustomerPreferenceRequest;
import com.project_aegis.user_service.dto.customer.request.CustomerUpdateRequest;
import com.project_aegis.user_service.dto.customer.response.CustomerPreferenceResponse;
import com.project_aegis.user_service.dto.customer.response.CustomerProfileResponse;
import com.project_aegis.user_service.dto.response.ApiResponse;
import com.project_aegis.user_service.service.CustomerAddressService;
import com.project_aegis.user_service.service.CustomerPreferenceService;
import com.project_aegis.user_service.service.CustomerProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/customers")
//api must be authenticated
public class CustomerProfileController {

    private final CustomerProfileService customerProfileService;
    private final CustomerAddressService customerAddressService;
    private final CustomerPreferenceService customerPreferenceService;

    /**
     * <p> </p>
     *
     * @param jwt jwt is fetched from user's jwt
     * @return {@link ApiResponse} containing {@link CustomerProfileResponse}
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<CustomerProfileResponse>> getCurrentUser(@AuthenticationPrincipal Jwt jwt) {
        String keycloakUserId = jwt.getSubject();
        return ResponseEntity.ok(customerProfileService.getCurrentCustomer(keycloakUserId));
    }

    /**
     * <p>Modify Existing Profile</p>
     *
     * @param jwt                   user auth
     * @param customerUpdateRequest update request
     * @return {@link CustomerProfileResponse}
     */
    @PatchMapping("/me")
    public ResponseEntity<ApiResponse<CustomerProfileResponse>> modifyCurrentUser(@AuthenticationPrincipal Jwt jwt,
                                                                                  @RequestBody CustomerUpdateRequest customerUpdateRequest) {
        String keycloakUserId = jwt.getSubject();
        return ResponseEntity.ok(customerProfileService.modifyCurrentUser(keycloakUserId, customerUpdateRequest));
    }

    /**
     * <p>Updating Existing Profile</p>
     *
     * @param jwt                   user auth
     * @param customerUpdateRequest update request
     * @return {@link CustomerProfileResponse}
     */
    @PutMapping("/me")
    public ResponseEntity<ApiResponse<CustomerProfileResponse>> updateCurrentUser(@AuthenticationPrincipal Jwt jwt,
                                                                                  @RequestBody CustomerUpdateRequest customerUpdateRequest) {
        String keycloakUserId = jwt.getSubject();
        return ResponseEntity.ok(customerProfileService.updateCurrentUser(keycloakUserId, customerUpdateRequest));
    }

    /**
     * <p>Adding address</p>
     *
     * @param jwt                    user auth
     * @param customerAddressRequest address request
     * @return {{@link CustomerAddressResponse}}
     */
    @PostMapping("/me/addresses")
    public ResponseEntity<ApiResponse<CustomerAddressResponse>> addCustomerAddress(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CustomerAddressRequest customerAddressRequest
    ) {
        String keycloakUserId = jwt.getSubject();

        return ResponseEntity.ok(customerAddressService.addCustomerAddress(keycloakUserId, customerAddressRequest));
    }


    @GetMapping("/me/addresses")
    public ResponseEntity<ApiResponse<List<CustomerAddressResponse>>> getCustomerAddress(
            @AuthenticationPrincipal Jwt jwt
    ) {
        String keycloakUserId = jwt.getSubject();
        return ResponseEntity.ok(customerAddressService.getCustomerAddress(keycloakUserId));
    }

    @GetMapping("me/addresses/{addressId}")
    public ResponseEntity<ApiResponse<CustomerAddressResponse>> getCustomerAddressByCustomerId(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable(name = "addressId") UUID addressId
    ) {
        String keycloakUserId = jwt.getSubject();
        return ResponseEntity.ok(customerAddressService.getCustomerAddressByCustomerId(keycloakUserId, addressId));
    }

    /**
     * <p>1.6 Update Existing Address (full replace)</p>
     */
    @PutMapping("/me/addresses/{addressId}")
    public ResponseEntity<CustomerAddressResponse> updateCustomerAddress(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable(name = "addressId") UUID addressId,
            @Valid @RequestBody CustomerAddressRequest customerAddressRequest
    ) {
        String keycloakUserId = jwt.getSubject();
        ApiResponse<CustomerAddressResponse> response =
                customerAddressService.updateCustomerAddress(keycloakUserId, addressId, customerAddressRequest);
        return ResponseEntity.ok(response.getData());
    }

    /**
     * <p>1.7 Delete Address</p>
     */
    @DeleteMapping("/me/addresses/{addressId}")
    public ResponseEntity<Void> deleteCustomerAddress(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable(name = "addressId") UUID addressId
    ) {
        String keycloakUserId = jwt.getSubject();
        customerAddressService.deleteCustomerAddress(keycloakUserId, addressId);
        return ResponseEntity.noContent().build();
    }

    /**
     * <p>1.8 Set Address as Default</p>
     */
    @PatchMapping("/me/addresses/{addressId}/default")
    public ResponseEntity<DefaultAddressResponse> setDefaultAddress(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable(name = "addressId") UUID addressId
    ) {
        String keycloakUserId = jwt.getSubject();
        return ResponseEntity.ok(customerAddressService.setDefaultAddress(keycloakUserId, addressId));
    }

    /**
     * <p>1.9 Get Preferences</p>
     */
    @GetMapping("/me/preferences")
    public ResponseEntity<CustomerPreferenceResponse> getPreferences(
            @AuthenticationPrincipal Jwt jwt
    ) {
        String keycloakUserId = jwt.getSubject();
        return ResponseEntity.ok(customerPreferenceService.getPreferences(keycloakUserId));
    }

    /**
     * <p>1.10 Update Preferences (Full Replace)</p>
     */
    @PutMapping("/me/preferences")
    public ResponseEntity<CustomerPreferenceResponse> replacePreferences(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody CustomerPreferenceRequest preferenceRequest
    ) {
        String keycloakUserId = jwt.getSubject();
        return ResponseEntity.ok(customerPreferenceService.replacePreferences(keycloakUserId, preferenceRequest));
    }

    /**
     * <p>1.11 Partial Update Preferences</p>
     */
    @PatchMapping("/me/preferences")
    public ResponseEntity<CustomerPreferenceResponse> updatePreferences(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody CustomerPreferenceRequest preferenceRequest
    ) {
        String keycloakUserId = jwt.getSubject();
        return ResponseEntity.ok(customerPreferenceService.updatePreferences(keycloakUserId, preferenceRequest));
    }
}

