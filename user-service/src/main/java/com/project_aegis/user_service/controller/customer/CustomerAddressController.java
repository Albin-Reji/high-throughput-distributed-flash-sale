package com.project_aegis.user_service.controller.customer;

import com.project_aegis.user_service.dto.address.response.CustomerAddressResponse;
import com.project_aegis.user_service.dto.address.response.DefaultAddressResponse;
import com.project_aegis.user_service.dto.customer.request.CustomerAddressRequest;
import com.project_aegis.user_service.dto.response.ApiResponse;
import com.project_aegis.user_service.service.CustomerAddressService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controller for managing customer addresses (/api/v1/customers/me/addresses).
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/customers/me/addresses")
public class CustomerAddressController {

    private final CustomerAddressService customerAddressService;

    /**
     * Add a new address for the authenticated customer.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<CustomerAddressResponse>> addCustomerAddress(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CustomerAddressRequest customerAddressRequest
    ) {
        String keycloakUserId = jwt.getSubject();
        return ResponseEntity.ok(customerAddressService.addCustomerAddress(keycloakUserId, customerAddressRequest));
    }

    /**
     * Get all addresses for the authenticated customer.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<CustomerAddressResponse>>> getCustomerAddress(
            @AuthenticationPrincipal Jwt jwt
    ) {
        String keycloakUserId = jwt.getSubject();
        return ResponseEntity.ok(customerAddressService.getCustomerAddress(keycloakUserId));
    }

    /**
     * Get specific address by ID for the authenticated customer.
     */
    @GetMapping("/{addressId}")
    public ResponseEntity<ApiResponse<CustomerAddressResponse>> getCustomerAddressByCustomerId(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable(name = "addressId") UUID addressId
    ) {
        String keycloakUserId = jwt.getSubject();
        return ResponseEntity.ok(customerAddressService.getCustomerAddressByCustomerId(keycloakUserId, addressId));
    }

    /**
     * Update an existing address (Full Replace).
     */
    @PutMapping("/{addressId}")
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
     * Delete an address by ID.
     */
    @DeleteMapping("/{addressId}")
    public ResponseEntity<Void> deleteCustomerAddress(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable(name = "addressId") UUID addressId
    ) {
        String keycloakUserId = jwt.getSubject();
        customerAddressService.deleteCustomerAddress(keycloakUserId, addressId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Set an address as default.
     */
    @PatchMapping("/{addressId}/default")
    public ResponseEntity<DefaultAddressResponse> setDefaultAddress(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable(name = "addressId") UUID addressId
    ) {
        String keycloakUserId = jwt.getSubject();
        return ResponseEntity.ok(customerAddressService.setDefaultAddress(keycloakUserId, addressId));
    }
}
