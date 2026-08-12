package com.project_aegis.user_service.service;

import com.project_aegis.user_service.dto.customer.request.CustomerUpdateRequest;
import com.project_aegis.user_service.dto.customer.response.CustomerProfileResponse;
import com.project_aegis.user_service.dto.request.StatusRequest;
import com.project_aegis.user_service.dto.response.ApiResponse;
import com.project_aegis.user_service.dto.response.PageResponse;
import com.project_aegis.user_service.dto.response.ProfileResponse;
import com.project_aegis.user_service.dto.response.StatusResponse;
import com.project_aegis.user_service.dto.webhook.KeycloakUserRegisteredEvent;
import com.project_aegis.user_service.entity.CustomerProfile;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * Service layer for managing customer profiles.
 */
public interface CustomerProfileService {

    /**
     * Creates a {@link CustomerProfile} from a Keycloak registration event.
     *
     * <p>This method is <b>idempotent</b>: if a profile with the same
     * {@code keycloakUserId} already exists, it returns the existing
     * profile without modification.</p>
     *
     * @param event the registration event payload
     * @return the created or existing customer profile
     */
    CustomerProfile createProfileFromKeycloakEvent(KeycloakUserRegisteredEvent event);


    ApiResponse<CustomerProfileResponse> getCurrentCustomer(String keycloakUserId);

    ApiResponse<CustomerProfileResponse> modifyCurrentUser(String keycloakUserId, @Valid CustomerUpdateRequest customerUpdateRequest);

    ApiResponse<CustomerProfileResponse> updateCurrentUser(String keycloakUserId, CustomerUpdateRequest customerUpdateRequest);

    PageResponse<CustomerProfileResponse> getAllCustomers(Pageable pageable);

    ApiResponse<ProfileResponse> getCustomerByCustomerId(UUID customerId);

    ApiResponse<StatusResponse> updateCustomerStatus(UUID customerId, StatusRequest request);

    ApiResponse<Void> deleteCustomerByAdmin(UUID customerId);
}
