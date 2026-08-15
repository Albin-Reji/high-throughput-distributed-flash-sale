package com.project_aegis.user_service.service;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CustomerProfileService Unit Tests")
class CustomerProfileServiceTest {

    @Mock
    private CustomerProfileRepository customerProfileRepository;

    @Mock
    private CustomerProfileMapper customerProfileMapper;

    @InjectMocks
    private CustomerProfileService customerProfileService;

    private static final String KEYCLOAK_USER_ID = "kc-user-001";
    private static final String EMAIL = "john@example.com";
    private static final String FIRST_NAME = "John";
    private static final String LAST_NAME = "Doe";

    private CustomerProfile testProfile;
    private UUID profileId;

    @BeforeEach
    void setUp() {
        profileId = UUID.randomUUID();
        Instant now = Instant.now();

        testProfile = CustomerProfile.builder()
                .id(profileId)
                .keycloakUserId(KEYCLOAK_USER_ID)
                .email(EMAIL)
                .firstName(FIRST_NAME)
                .lastName(LAST_NAME)
                .accountStatus(AccountStatus.ACTIVE)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    // ──────────────────────────────────────────────────────────────
    //  createProfileFromKeycloakEvent
    // ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("createProfileFromKeycloakEvent")
    class CreateProfileFromKeycloakEvent {

        private KeycloakUserRegisteredEvent event;

        @BeforeEach
        void setUp() {
            event = KeycloakUserRegisteredEvent.builder()
                    .keycloakUserId(KEYCLOAK_USER_ID)
                    .email(EMAIL)
                    .firstName(FIRST_NAME)
                    .lastName(LAST_NAME)
                    .build();
        }

        @Test
        @DisplayName("should create a new profile when none exists")
        void shouldCreateNewProfile() {
            when(customerProfileRepository.findByKeycloakUserId(KEYCLOAK_USER_ID))
                    .thenReturn(Optional.empty());
            when(customerProfileRepository.save(any(CustomerProfile.class)))
                    .thenReturn(testProfile);

            CustomerProfile result = customerProfileService.createProfileFromKeycloakEvent(event);

            assertThat(result).isNotNull();
            assertThat(result.getKeycloakUserId()).isEqualTo(KEYCLOAK_USER_ID);
            assertThat(result.getEmail()).isEqualTo(EMAIL);

            ArgumentCaptor<CustomerProfile> captor = ArgumentCaptor.forClass(CustomerProfile.class);
            verify(customerProfileRepository).save(captor.capture());

            CustomerProfile savedProfile = captor.getValue();
            assertThat(savedProfile.getKeycloakUserId()).isEqualTo(KEYCLOAK_USER_ID);
            assertThat(savedProfile.getEmail()).isEqualTo(EMAIL);
            assertThat(savedProfile.getFirstName()).isEqualTo(FIRST_NAME);
            assertThat(savedProfile.getLastName()).isEqualTo(LAST_NAME);
            assertThat(savedProfile.getAccountStatus()).isEqualTo(AccountStatus.ACTIVE);
            assertThat(savedProfile.getPreference()).isNotNull();
            assertThat(savedProfile.getPreference().getPreferredCurrency()).isEqualTo("INR");
            assertThat(savedProfile.getPreference().isMarketingEmailsEnabled()).isFalse();
            assertThat(savedProfile.getPreference().isSmsNotificationsEnabled()).isFalse();
        }

        @Test
        @DisplayName("should return existing profile when keycloakUserId already exists (idempotent)")
        void shouldReturnExistingProfile() {
            when(customerProfileRepository.findByKeycloakUserId(KEYCLOAK_USER_ID))
                    .thenReturn(Optional.of(testProfile));

            CustomerProfile result = customerProfileService.createProfileFromKeycloakEvent(event);

            assertThat(result).isSameAs(testProfile);
            verify(customerProfileRepository, never()).save(any());
        }
    }

    // ──────────────────────────────────────────────────────────────
    //  getCurrentCustomer
    // ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getCurrentCustomer")
    class GetCurrentCustomer {

        @Test
        @DisplayName("should return profile for valid keycloakUserId")
        void shouldReturnProfile() {
            CustomerProfileResponse expectedResponse = CustomerProfileResponse.builder()
                    .id(profileId)
                    .email(EMAIL)
                    .firstName(FIRST_NAME)
                    .lastName(LAST_NAME)
                    .accountStatus(AccountStatus.ACTIVE)
                    .build();

            when(customerProfileRepository.findByKeycloakUserId(KEYCLOAK_USER_ID))
                    .thenReturn(Optional.of(testProfile));
            when(customerProfileMapper.toResponse(testProfile))
                    .thenReturn(expectedResponse);

            ApiResponse<CustomerProfileResponse> result =
                    customerProfileService.getCurrentCustomer(KEYCLOAK_USER_ID);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getMessage()).isEqualTo("Customer profile retrieved successfully");
            assertThat(result.getData()).isEqualTo(expectedResponse);
        }

        @Test
        @DisplayName("should throw CustomerNotFoundException when profile not found")
        void shouldThrowWhenNotFound() {
            when(customerProfileRepository.findByKeycloakUserId(KEYCLOAK_USER_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> customerProfileService.getCurrentCustomer(KEYCLOAK_USER_ID))
                    .isInstanceOf(CustomerNotFoundException.class)
                    .hasMessageContaining("Customer profile not found");
        }
    }

    // ──────────────────────────────────────────────────────────────
    //  modifyCurrentUser (PATCH — partial update)
    // ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("modifyCurrentUser (partial update)")
    class ModifyCurrentUser {

        @Test
        @DisplayName("should partially update and return updated profile")
        void shouldPartiallyUpdate() {
            CustomerUpdateRequest request = CustomerUpdateRequest.builder()
                    .firstName("Jane")
                    .build();

            CustomerProfileResponse expectedResponse = CustomerProfileResponse.builder()
                    .id(profileId)
                    .email(EMAIL)
                    .firstName("Jane")
                    .lastName(LAST_NAME)
                    .accountStatus(AccountStatus.ACTIVE)
                    .build();

            when(customerProfileRepository.findByKeycloakUserId(KEYCLOAK_USER_ID))
                    .thenReturn(Optional.of(testProfile));
            when(customerProfileRepository.save(testProfile))
                    .thenReturn(testProfile);
            when(customerProfileMapper.toResponse(testProfile))
                    .thenReturn(expectedResponse);

            ApiResponse<CustomerProfileResponse> result =
                    customerProfileService.modifyCurrentUser(KEYCLOAK_USER_ID, request);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getMessage()).isEqualTo("Customer profile updated successfully");
            assertThat(result.getData().firstName()).isEqualTo("Jane");
            verify(customerProfileMapper).replaceEntity(request, testProfile);
            verify(customerProfileRepository).save(testProfile);
        }

        @Test
        @DisplayName("should throw CustomerNotFoundException when profile not found")
        void shouldThrowWhenNotFound() {
            CustomerUpdateRequest request = CustomerUpdateRequest.builder().build();

            when(customerProfileRepository.findByKeycloakUserId(KEYCLOAK_USER_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> customerProfileService.modifyCurrentUser(KEYCLOAK_USER_ID, request))
                    .isInstanceOf(CustomerNotFoundException.class);
        }
    }

    // ──────────────────────────────────────────────────────────────
    //  updateCurrentUser (PUT — full update)
    // ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("updateCurrentUser (full update)")
    class UpdateCurrentUser {

        @Test
        @DisplayName("should fully update and return updated profile")
        void shouldFullyUpdate() {
            CustomerUpdateRequest request = CustomerUpdateRequest.builder()
                    .email("jane@example.com")
                    .firstName("Jane")
                    .lastName("Smith")
                    .phoneNumber("9876543210")
                    .build();

            CustomerProfileResponse expectedResponse = CustomerProfileResponse.builder()
                    .id(profileId)
                    .email("jane@example.com")
                    .firstName("Jane")
                    .lastName("Smith")
                    .phoneNumber("9876543210")
                    .accountStatus(AccountStatus.ACTIVE)
                    .build();

            when(customerProfileRepository.findByKeycloakUserId(KEYCLOAK_USER_ID))
                    .thenReturn(Optional.of(testProfile));
            when(customerProfileRepository.save(testProfile))
                    .thenReturn(testProfile);
            when(customerProfileMapper.toResponse(testProfile))
                    .thenReturn(expectedResponse);

            ApiResponse<CustomerProfileResponse> result =
                    customerProfileService.updateCurrentUser(KEYCLOAK_USER_ID, request);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData().email()).isEqualTo("jane@example.com");
            verify(customerProfileMapper).updateEntity(request, testProfile);
        }

        @Test
        @DisplayName("should throw CustomerNotFoundException when profile not found")
        void shouldThrowWhenNotFound() {
            CustomerUpdateRequest request = CustomerUpdateRequest.builder().build();

            when(customerProfileRepository.findByKeycloakUserId(KEYCLOAK_USER_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> customerProfileService.updateCurrentUser(KEYCLOAK_USER_ID, request))
                    .isInstanceOf(CustomerNotFoundException.class);
        }
    }

    // ──────────────────────────────────────────────────────────────
    //  getAllCustomers
    // ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getAllCustomers")
    class GetAllCustomers {

        @Test
        @DisplayName("should return paginated list of customers")
        void shouldReturnPaginatedCustomers() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<CustomerProfile> profilePage = new PageImpl<>(
                    List.of(testProfile), pageable, 1
            );

            CustomerProfileResponse expectedResponse = CustomerProfileResponse.builder()
                    .id(profileId)
                    .email(EMAIL)
                    .firstName(FIRST_NAME)
                    .lastName(LAST_NAME)
                    .accountStatus(AccountStatus.ACTIVE)
                    .build();

            when(customerProfileRepository.findAll(pageable)).thenReturn(profilePage);
            when(customerProfileMapper.toResponse(testProfile)).thenReturn(expectedResponse);

            PageResponse<CustomerProfileResponse> result =
                    customerProfileService.getAllCustomers(pageable);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().getFirst()).isEqualTo(expectedResponse);
            assertThat(result.getPage()).isZero();
            assertThat(result.getSize()).isEqualTo(10);
            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getTotalPages()).isEqualTo(1);
            assertThat(result.isFirst()).isTrue();
            assertThat(result.isLast()).isTrue();
        }

        @Test
        @DisplayName("should return empty page when no customers exist")
        void shouldReturnEmptyPage() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<CustomerProfile> emptyPage = new PageImpl<>(List.of(), pageable, 0);

            when(customerProfileRepository.findAll(pageable)).thenReturn(emptyPage);

            PageResponse<CustomerProfileResponse> result =
                    customerProfileService.getAllCustomers(pageable);

            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isZero();
        }
    }

    // ──────────────────────────────────────────────────────────────
    //  getCustomerByCustomerId
    // ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getCustomerByCustomerId")
    class GetCustomerByCustomerId {

        @Test
        @DisplayName("should return full profile response for valid customerId")
        void shouldReturnFullProfile() {
            CustomerPreference preference = CustomerPreference.builder()
                    .customer(testProfile)
                    .marketingEmailsEnabled(true)
                    .smsNotificationsEnabled(false)
                    .preferredCurrency("USD")
                    .build();
            testProfile.setPreference(preference);

            ProfileResponse expectedResponse = ProfileResponse.builder()
                    .id(profileId)
                    .email(EMAIL)
                    .firstName(FIRST_NAME)
                    .lastName(LAST_NAME)
                    .accountStatus(AccountStatus.ACTIVE)
                    .build();

            when(customerProfileRepository.findById(profileId))
                    .thenReturn(Optional.of(testProfile));
            when(customerProfileMapper.toProfileResponse(testProfile))
                    .thenReturn(expectedResponse);

            ApiResponse<ProfileResponse> result =
                    customerProfileService.getCustomerByCustomerId(profileId);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getMessage()).isEqualTo("Customer profile retrieved successfully");
            assertThat(result.getData()).isEqualTo(expectedResponse);
        }

        @Test
        @DisplayName("should throw CustomerNotFoundException when customerId not found")
        void shouldThrowWhenNotFound() {
            UUID unknownId = UUID.randomUUID();
            when(customerProfileRepository.findById(unknownId))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> customerProfileService.getCustomerByCustomerId(unknownId))
                    .isInstanceOf(CustomerNotFoundException.class);
        }
    }

    // ──────────────────────────────────────────────────────────────
    //  updateCustomerStatus
    // ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("updateCustomerStatus")
    class UpdateCustomerStatus {

        @Test
        @DisplayName("should update status and return StatusResponse")
        void shouldUpdateStatus() {
            StatusRequest request = new StatusRequest();
            request.setAccountStatus(AccountStatus.SUSPENDED);

            Instant updatedAt = Instant.now();
            CustomerProfile savedProfile = CustomerProfile.builder()
                    .id(profileId)
                    .keycloakUserId(KEYCLOAK_USER_ID)
                    .email(EMAIL)
                    .firstName(FIRST_NAME)
                    .lastName(LAST_NAME)
                    .accountStatus(AccountStatus.SUSPENDED)
                    .updatedAt(updatedAt)
                    .build();

            when(customerProfileRepository.findById(profileId))
                    .thenReturn(Optional.of(testProfile));
            when(customerProfileRepository.save(testProfile))
                    .thenReturn(savedProfile);

            ApiResponse<StatusResponse> result =
                    customerProfileService.updateCustomerStatus(profileId, request);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getMessage()).isEqualTo("Customer profile status  updated successfully");
            assertThat(result.getData().getAccountStatus()).isEqualTo(AccountStatus.SUSPENDED);
            assertThat(result.getData().getCustomerId()).isEqualTo(profileId);
            // Verify the status was set on the profile before saving
            assertThat(testProfile.getAccountStatus()).isEqualTo(AccountStatus.SUSPENDED);
            verify(customerProfileRepository).save(testProfile);
        }

        @Test
        @DisplayName("should throw CustomerNotFoundException when customerId not found")
        void shouldThrowWhenNotFound() {
            StatusRequest request = new StatusRequest();
            request.setAccountStatus(AccountStatus.INACTIVE);

            when(customerProfileRepository.findById(profileId))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> customerProfileService.updateCustomerStatus(profileId, request))
                    .isInstanceOf(CustomerNotFoundException.class);
        }
    }

    // ──────────────────────────────────────────────────────────────
    //  deleteCustomerByAdmin
    // ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("deleteCustomerByAdmin")
    class DeleteCustomerByAdmin {

        @Test
        @DisplayName("should delete profile and return success response")
        void shouldDeleteProfile() {
            when(customerProfileRepository.findById(profileId))
                    .thenReturn(Optional.of(testProfile));

            ApiResponse<Void> result = customerProfileService.deleteCustomerByAdmin(profileId);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getMessage()).isEqualTo("customer profile deleted successfully");
            verify(customerProfileRepository).delete(testProfile);
        }

        @Test
        @DisplayName("should throw CustomerNotFoundException when customerId not found")
        void shouldThrowWhenNotFound() {
            UUID unknownId = UUID.randomUUID();
            when(customerProfileRepository.findById(unknownId))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> customerProfileService.deleteCustomerByAdmin(unknownId))
                    .isInstanceOf(CustomerNotFoundException.class);
        }
    }
}