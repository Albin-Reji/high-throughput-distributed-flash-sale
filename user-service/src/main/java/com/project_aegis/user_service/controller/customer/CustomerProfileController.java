package com.project_aegis.user_service.controller.customer;

import com.project_aegis.user_service.dto.address.response.CustomerAddressResponse;
import com.project_aegis.user_service.dto.customer.request.CustomerAddressRequest;
import com.project_aegis.user_service.dto.customer.request.CustomerUpdateRequest;
import com.project_aegis.user_service.dto.customer.response.CustomerProfileResponse;
import com.project_aegis.user_service.dto.response.ApiResponse;
import com.project_aegis.user_service.service.CustomerAddressService;
import com.project_aegis.user_service.service.CustomerProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/customers")
//api must be authenticated
public class CustomerProfileController {

    private final CustomerProfileService customerProfileService;
    private final CustomerAddressService customerAddressService;

    /**
     * <p> </p>
     * @param jwt jwt is fetched from user's jwt
     * @return {@link ApiResponse} containing {@link CustomerProfileResponse}
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<CustomerProfileResponse>> getCurrentUser(@AuthenticationPrincipal Jwt jwt) {
        String keycloakUserId=jwt.getSubject();
        return ResponseEntity.ok(customerProfileService.getCurrentCustomer(keycloakUserId));
    }

    /**
     * <p>Modify Existing Profile</p>
     * @param jwt user auth
     * @param customerUpdateRequest update request
     * @return {@link CustomerProfileResponse}
     */
    @PatchMapping("/me")
    public ResponseEntity<ApiResponse<CustomerProfileResponse>> modifyCurrentUser(@AuthenticationPrincipal Jwt jwt,
                                                                     @RequestBody CustomerUpdateRequest customerUpdateRequest) {
        String keycloakUserId=jwt.getSubject();
        return ResponseEntity.ok(customerProfileService.modifyCurrentUser(keycloakUserId, customerUpdateRequest));
    }
    @PutMapping("/me")
    public ResponseEntity<ApiResponse<CustomerProfileResponse>> updateCurrentUser(@AuthenticationPrincipal Jwt jwt,
                                                                                  @RequestBody CustomerUpdateRequest customerUpdateRequest) {
        String keycloakUserId=jwt.getSubject();
        return ResponseEntity.ok(customerProfileService.updateCurrentUser(keycloakUserId, customerUpdateRequest));
    }
    @PostMapping("/me/addresses")
    public ResponseEntity<ApiResponse<CustomerAddressResponse>> addCustomerAddress(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CustomerAddressRequest customerAddressRequest
    ){
        String keycloakUserId=jwt.getSubject();

        return ResponseEntity.ok(customerAddressService.addCustomerAddress(keycloakUserId, customerAddressRequest));
    }

}
