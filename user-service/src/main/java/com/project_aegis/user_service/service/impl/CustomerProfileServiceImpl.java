package com.project_aegis.user_service.service.impl;

import com.project_aegis.user_service.dto.customer.request.CustomerUpdateRequest;
import com.project_aegis.user_service.dto.customer.response.CustomerProfileResponse;
import com.project_aegis.user_service.dto.request.StatusRequest;
import com.project_aegis.user_service.dto.response.ApiResponse;
import com.project_aegis.user_service.dto.response.PageResponse;
import com.project_aegis.user_service.dto.response.ProfileResponse;
import com.project_aegis.user_service.dto.response.StatusResponse;
import com.project_aegis.user_service.dto.webhook.KeycloakUserRegisteredEvent;
import com.project_aegis.user_service.entity.AccountStatus;
import com.project_aegis.user_service.entity.CustomerPreference;
import com.project_aegis.user_service.entity.CustomerProfile;
import com.project_aegis.user_service.exception.CustomerNotFoundException;
import com.project_aegis.user_service.mapper.CustomerProfileMapper;
import com.project_aegis.user_service.repository.CustomerProfileRepository;
import com.project_aegis.user_service.service.CustomerProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.hibernate.query.Page.first;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerProfileServiceImpl implements CustomerProfileService {

    private final CustomerProfileRepository customerProfileRepository;
    private final CustomerProfileMapper customerProfileMapper;



    /**
     * Creates a new {@link CustomerProfile} from a Keycloak registration event,
     * or returns the existing one if a profile with the same keycloakUserId
     * already exists (idempotent).
     */
    @Override
    @Transactional
    public CustomerProfile createProfileFromKeycloakEvent(KeycloakUserRegisteredEvent event) {

        // Idempotency check — if the profile already exists, return it
        Optional<CustomerProfile> existing =
                customerProfileRepository.findByKeycloakUserId(event.keycloakUserId());

        if (existing.isPresent()) {
            log.info("CustomerProfile already exists for keycloakUserId={}, skipping creation",
                    event.keycloakUserId());
            return existing.get();
        }

        // Build the new profile
        CustomerProfile profile = CustomerProfile.builder()
                .keycloakUserId(event.keycloakUserId())
                .email(event.email())
                .firstName(event.firstName())
                .lastName(event.lastName())
                .accountStatus(AccountStatus.ACTIVE)
                .build();

        // Build default preferences and link them
        CustomerPreference preference = CustomerPreference.builder()
                .customer(profile)
                .marketingEmailsEnabled(false)
                .smsNotificationsEnabled(false)
                .preferredCurrency("INR")
                .build();

        profile.setPreference(preference);

        CustomerProfile saved = customerProfileRepository.save(profile);

        log.info("CustomerProfile created: id={}, keycloakUserId={}, email={}",
                saved.getId(), saved.getKeycloakUserId(), saved.getEmail());

        return saved;
    }

    @Override
    public ApiResponse<CustomerProfileResponse> getCurrentCustomer(String keycloakUserId) {
        // checking is user customer is present
        CustomerProfile customerProfile =customerProfileRepository.findByKeycloakUserId(keycloakUserId)
                .orElseThrow(() -> new CustomerNotFoundException("Customer profile not found "));

        return ApiResponse.<CustomerProfileResponse>builder()
                .success(true)
                .message("Customer profile retrieved successfully")
                .data(customerProfileMapper.toResponse(customerProfile))
                .build();


    }

    /**
     * <p>Partial Update</p>
     * @param keycloakUserId unique userId
     * @param customerUpdateRequest customerUpdateRequest structure
     * @return {@link ApiResponse} containing  {@link CustomerProfileResponse}
     */

    @Override
    @Transactional
    public ApiResponse<CustomerProfileResponse> modifyCurrentUser(String keycloakUserId,
                                                                  CustomerUpdateRequest customerUpdateRequest) {

        CustomerProfile customerProfile =customerProfileRepository.findByKeycloakUserId(keycloakUserId)
                .orElseThrow(()->new CustomerNotFoundException("Customer profile not found "));

        customerProfileMapper.replaceEntity(customerUpdateRequest, customerProfile);

        customerProfileRepository.save(customerProfile);
        return ApiResponse.<CustomerProfileResponse>builder()
                .success(true)
                .message("Customer profile updated successfully")
                .data(customerProfileMapper.toResponse(customerProfile))
                .build();

    }

    @Override
    @Transactional
    public ApiResponse<CustomerProfileResponse> updateCurrentUser(String keycloakUserId,
                                                                  CustomerUpdateRequest customerUpdateRequest) {
        CustomerProfile customerProfile =customerProfileRepository.findByKeycloakUserId(keycloakUserId)
                .orElseThrow(()->new CustomerNotFoundException("Customer profile not found "));

        customerProfileMapper.updateEntity(customerUpdateRequest, customerProfile);
        customerProfileRepository.save(customerProfile);
        return ApiResponse.<CustomerProfileResponse>builder()
                .success(true)
                .message("Customer profile updated successfully")
                .data(customerProfileMapper.toResponse(customerProfile))
                .build();
    }

    @Override
    public PageResponse<CustomerProfileResponse> getAllCustomers(Pageable pageable) {
        Page<CustomerProfile> profiles=customerProfileRepository.findAll(pageable);

        return PageResponse.<CustomerProfileResponse>builder()
                .content(profiles.stream()
                        .map(customerProfileMapper::toResponse)
                        .collect(Collectors.toCollection(ArrayList::new))
                )
                .first(profiles.isFirst())
                .last(profiles.isLast())
                .page(pageable.getPageNumber())
                .size(profiles.getSize())
                .totalElements(profiles.getTotalElements())
                .totalPages(profiles.getTotalPages())
                .build();
    }

    @Override
    public ApiResponse<ProfileResponse> getCustomerByCustomerId(UUID customerId) {
        CustomerProfile profile =customerProfileRepository.findById(customerId)
                .orElseThrow(()-> new CustomerNotFoundException("Customer profile not found "));

        return ApiResponse.<ProfileResponse>builder()
                .success(true)
                .message("Customer profile retrieved successfully")
                .data(customerProfileMapper.toProfileResponse(profile))
                .build();

    }

    @Override
    @Transactional
    public ApiResponse<StatusResponse> updateCustomerStatus(UUID customerId, StatusRequest request) {
        CustomerProfile profile=customerProfileRepository.findById(customerId)
                .orElseThrow(()->new CustomerNotFoundException("Customer profile not found "));

        profile.setAccountStatus(request.getAccountStatus());


        CustomerProfile savedProfile=customerProfileRepository.save(profile);

        return ApiResponse.<StatusResponse>builder()
                .message("Customer profile status  updated successfully")
                .success(true)
                .data(StatusResponse.builder()
                        .accountStatus(savedProfile.getAccountStatus())
                        .customerId(customerId)
                        .updatedAt(savedProfile.getUpdatedAt())
                        .build())
                .build();
    }

    @Override
    public ApiResponse<Void> deleteCustomerByAdmin(UUID customerId) {
        CustomerProfile profile=customerProfileRepository.findById(customerId)
                .orElseThrow(()->new CustomerNotFoundException("Customer profile not found "));

        customerProfileRepository.delete(profile);
        return ApiResponse.<Void>builder()
                .success(true)
                .message("customer profile deleted successfullyc")
                .build();
    }
}
