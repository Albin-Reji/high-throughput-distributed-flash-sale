package com.project_aegis.user_service.service;

import com.project_aegis.user_service.dto.address.request.CustomerAddressRequest;
import com.project_aegis.user_service.dto.address.response.CustomerAddressResponse;
import com.project_aegis.user_service.dto.address.response.DefaultAddressResponse;
import com.project_aegis.user_service.dto.response.ApiResponse;
import com.project_aegis.user_service.entity.AddressType;
import com.project_aegis.user_service.entity.CustomerAddress;
import com.project_aegis.user_service.entity.CustomerProfile;
import com.project_aegis.user_service.exception.AddressNotFoundException;
import com.project_aegis.user_service.exception.CustomerNotFoundException;
import com.project_aegis.user_service.mapper.CustomerAddressMapper;
import com.project_aegis.user_service.repository.CustomerAddressRepository;
import com.project_aegis.user_service.repository.CustomerProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CustomerAddressService Unit Tests")
class CustomerAddressServiceTest {

    @Mock
    private CustomerAddressRepository customerAddressRepository;

    @Mock
    private CustomerProfileRepository customerProfileRepository;

    @Mock
    private CustomerAddressMapper customerAddressMapper;

    @InjectMocks
    private CustomerAddressService customerAddressService;

    private static final String KEYCLOAK_USER_ID = "kc-user-001";

    private CustomerProfile testProfile;
    private CustomerAddress testAddress;
    private CustomerAddressResponse testAddressResponse;
    private CustomerAddressRequest testAddressRequest;
    private UUID profileId;
    private UUID addressId;

    @BeforeEach
    void setUp() {
        profileId = UUID.randomUUID();
        addressId = UUID.randomUUID();

        testProfile = CustomerProfile.builder()
                .id(profileId)
                .keycloakUserId(KEYCLOAK_USER_ID)
                .email("john@example.com")
                .firstName("John")
                .lastName("Doe")
                .build();

        testAddress = CustomerAddress.builder()
                .id(addressId)
                .customer(testProfile)
                .addressType(AddressType.DELIVERY)
                .addressLine1("123 Main St")
                .addressLine2("Apt 4B")
                .city("Mumbai")
                .state("Maharashtra")
                .postalCode("400001")
                .country("India")
                .defaultAddress(false)
                .build();

        testAddressResponse = CustomerAddressResponse.builder()
                .id(addressId)
                .addressType(AddressType.DELIVERY)
                .addressLine1("123 Main St")
                .addressLine2("Apt 4B")
                .city("Mumbai")
                .state("Maharashtra")
                .postalCode("400001")
                .country("India")
                .defaultAddress(false)
                .build();

        testAddressRequest = CustomerAddressRequest.builder()
                .addressType(AddressType.DELIVERY)
                .addressLine1("123 Main St")
                .addressLine2("Apt 4B")
                .city("Mumbai")
                .state("Maharashtra")
                .postalCode("400001")
                .country("India")
                .defaultAddress(false)
                .build();
    }

    // ──────────────────────────────────────────────────────────────
    //  addCustomerAddress
    // ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("addCustomerAddress")
    class AddCustomerAddress {

        @Test
        @DisplayName("should add address and return success response")
        void shouldAddAddress() {
            when(customerProfileRepository.findByKeycloakUserId(KEYCLOAK_USER_ID))
                    .thenReturn(Optional.of(testProfile));
            when(customerAddressMapper.toEntity(testAddressRequest))
                    .thenReturn(testAddress);
            when(customerAddressRepository.save(testAddress))
                    .thenReturn(testAddress);
            when(customerAddressMapper.toResponse(testAddress))
                    .thenReturn(testAddressResponse);

            ApiResponse<CustomerAddressResponse> result =
                    customerAddressService.addCustomerAddress(KEYCLOAK_USER_ID, testAddressRequest);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getMessage()).isEqualTo("Customer address added successfully");
            assertThat(result.getData().getAddressLine1()).isEqualTo("123 Main St");
            assertThat(result.getData().getCity()).isEqualTo("Mumbai");
            verify(customerAddressRepository).save(testAddress);
        }

        @Test
        @DisplayName("should set customer on address before saving")
        void shouldSetCustomerOnAddress() {
            CustomerAddress newAddress = CustomerAddress.builder().build();

            when(customerProfileRepository.findByKeycloakUserId(KEYCLOAK_USER_ID))
                    .thenReturn(Optional.of(testProfile));
            when(customerAddressMapper.toEntity(testAddressRequest))
                    .thenReturn(newAddress);
            when(customerAddressRepository.save(newAddress))
                    .thenReturn(newAddress);
            when(customerAddressMapper.toResponse(newAddress))
                    .thenReturn(testAddressResponse);

            customerAddressService.addCustomerAddress(KEYCLOAK_USER_ID, testAddressRequest);

            assertThat(newAddress.getCustomer()).isSameAs(testProfile);
        }

        @Test
        @DisplayName("should throw CustomerNotFoundException when customer not found")
        void shouldThrowWhenCustomerNotFound() {
            when(customerProfileRepository.findByKeycloakUserId(KEYCLOAK_USER_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    customerAddressService.addCustomerAddress(KEYCLOAK_USER_ID, testAddressRequest))
                    .isInstanceOf(CustomerNotFoundException.class);

            verify(customerAddressRepository, never()).save(any());
        }
    }

    // ──────────────────────────────────────────────────────────────
    //  getCustomerAddress (list all)
    // ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getCustomerAddress")
    class GetCustomerAddress {

        @Test
        @DisplayName("should return list of addresses for customer")
        void shouldReturnAddresses() {
            when(customerProfileRepository.findByKeycloakUserId(KEYCLOAK_USER_ID))
                    .thenReturn(Optional.of(testProfile));
            when(customerAddressRepository.findAllByCustomerId(profileId))
                    .thenReturn(List.of(testAddress));
            when(customerAddressMapper.toResponse(testAddress))
                    .thenReturn(testAddressResponse);

            ApiResponse<List<CustomerAddressResponse>> result =
                    customerAddressService.getCustomerAddress(KEYCLOAK_USER_ID);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).hasSize(1);
            assertThat(result.getData().getFirst().getCity()).isEqualTo("Mumbai");
        }

        @Test
        @DisplayName("should throw AddressNotFoundException when no addresses exist")
        void shouldThrowWhenNoAddresses() {
            when(customerProfileRepository.findByKeycloakUserId(KEYCLOAK_USER_ID))
                    .thenReturn(Optional.of(testProfile));
            when(customerAddressRepository.findAllByCustomerId(profileId))
                    .thenReturn(List.of());

            assertThatThrownBy(() ->
                    customerAddressService.getCustomerAddress(KEYCLOAK_USER_ID))
                    .isInstanceOf(AddressNotFoundException.class)
                    .hasMessageContaining("No address  found for this user");
        }

        @Test
        @DisplayName("should throw CustomerNotFoundException when customer not found")
        void shouldThrowWhenCustomerNotFound() {
            when(customerProfileRepository.findByKeycloakUserId(KEYCLOAK_USER_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    customerAddressService.getCustomerAddress(KEYCLOAK_USER_ID))
                    .isInstanceOf(CustomerNotFoundException.class);
        }
    }

    // ──────────────────────────────────────────────────────────────
    //  getCustomerAddressByCustomerId (single address)
    // ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getCustomerAddressByCustomerId")
    class GetCustomerAddressByCustomerId {

        @Test
        @DisplayName("should return address when it belongs to the customer")
        void shouldReturnAddressWhenOwned() {
            when(customerProfileRepository.findByKeycloakUserId(KEYCLOAK_USER_ID))
                    .thenReturn(Optional.of(testProfile));
            when(customerAddressRepository.findById(addressId))
                    .thenReturn(Optional.of(testAddress));
            when(customerAddressMapper.toResponse(testAddress))
                    .thenReturn(testAddressResponse);

            ApiResponse<CustomerAddressResponse> result =
                    customerAddressService.getCustomerAddressByCustomerId(KEYCLOAK_USER_ID, addressId);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getMessage()).isEqualTo("Address Found");
            assertThat(result.getData().getId()).isEqualTo(addressId);
        }

        @Test
        @DisplayName("should throw AddressNotFoundException when address belongs to another customer")
        void shouldThrowWhenAddressBelongsToAnotherCustomer() {
            CustomerProfile otherProfile = CustomerProfile.builder()
                    .id(UUID.randomUUID())
                    .build();
            CustomerAddress otherAddress = CustomerAddress.builder()
                    .id(addressId)
                    .customer(otherProfile)
                    .build();

            when(customerProfileRepository.findByKeycloakUserId(KEYCLOAK_USER_ID))
                    .thenReturn(Optional.of(testProfile));
            when(customerAddressRepository.findById(addressId))
                    .thenReturn(Optional.of(otherAddress));

            assertThatThrownBy(() ->
                    customerAddressService.getCustomerAddressByCustomerId(KEYCLOAK_USER_ID, addressId))
                    .isInstanceOf(AddressNotFoundException.class);
        }

        @Test
        @DisplayName("should throw AddressNotFoundException when address not found")
        void shouldThrowWhenAddressNotFound() {
            when(customerProfileRepository.findByKeycloakUserId(KEYCLOAK_USER_ID))
                    .thenReturn(Optional.of(testProfile));
            when(customerAddressRepository.findById(addressId))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    customerAddressService.getCustomerAddressByCustomerId(KEYCLOAK_USER_ID, addressId))
                    .isInstanceOf(AddressNotFoundException.class);
        }

        @Test
        @DisplayName("should throw CustomerNotFoundException when customer not found")
        void shouldThrowWhenCustomerNotFound() {
            when(customerProfileRepository.findByKeycloakUserId(KEYCLOAK_USER_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    customerAddressService.getCustomerAddressByCustomerId(KEYCLOAK_USER_ID, addressId))
                    .isInstanceOf(CustomerNotFoundException.class);
        }
    }

    // ──────────────────────────────────────────────────────────────
    //  updateCustomerAddress
    // ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("updateCustomerAddress")
    class UpdateCustomerAddress {

        @Test
        @DisplayName("should update address and return success response")
        void shouldUpdateAddress() {
            CustomerAddressRequest updateRequest = CustomerAddressRequest.builder()
                    .addressType(AddressType.BILLING)
                    .addressLine1("456 New St")
                    .city("Delhi")
                    .state("Delhi")
                    .postalCode("110001")
                    .country("India")
                    .build();

            CustomerAddressResponse updatedResponse = CustomerAddressResponse.builder()
                    .id(addressId)
                    .addressType(AddressType.BILLING)
                    .addressLine1("456 New St")
                    .city("Delhi")
                    .build();

            when(customerProfileRepository.findByKeycloakUserId(KEYCLOAK_USER_ID))
                    .thenReturn(Optional.of(testProfile));
            when(customerAddressRepository.findByIdAndCustomerId(addressId, profileId))
                    .thenReturn(Optional.of(testAddress));
            when(customerAddressRepository.save(testAddress))
                    .thenReturn(testAddress);
            when(customerAddressMapper.toResponse(testAddress))
                    .thenReturn(updatedResponse);

            ApiResponse<CustomerAddressResponse> result =
                    customerAddressService.updateCustomerAddress(KEYCLOAK_USER_ID, addressId, updateRequest);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getMessage()).isEqualTo("Customer address updated successfully");
            verify(customerAddressMapper).updateEntity(updateRequest, testAddress);
            verify(customerAddressRepository).save(testAddress);
        }

        @Test
        @DisplayName("should throw AddressNotFoundException when address not found for customer")
        void shouldThrowWhenAddressNotFound() {
            when(customerProfileRepository.findByKeycloakUserId(KEYCLOAK_USER_ID))
                    .thenReturn(Optional.of(testProfile));
            when(customerAddressRepository.findByIdAndCustomerId(addressId, profileId))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    customerAddressService.updateCustomerAddress(KEYCLOAK_USER_ID, addressId, testAddressRequest))
                    .isInstanceOf(AddressNotFoundException.class);
        }

        @Test
        @DisplayName("should throw CustomerNotFoundException when customer not found")
        void shouldThrowWhenCustomerNotFound() {
            when(customerProfileRepository.findByKeycloakUserId(KEYCLOAK_USER_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    customerAddressService.updateCustomerAddress(KEYCLOAK_USER_ID, addressId, testAddressRequest))
                    .isInstanceOf(CustomerNotFoundException.class);
        }
    }

    // ──────────────────────────────────────────────────────────────
    //  deleteCustomerAddress
    // ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("deleteCustomerAddress")
    class DeleteCustomerAddress {

        @Test
        @DisplayName("should delete address successfully")
        void shouldDeleteAddress() {
            when(customerProfileRepository.findByKeycloakUserId(KEYCLOAK_USER_ID))
                    .thenReturn(Optional.of(testProfile));
            when(customerAddressRepository.findByIdAndCustomerId(addressId, profileId))
                    .thenReturn(Optional.of(testAddress));

            customerAddressService.deleteCustomerAddress(KEYCLOAK_USER_ID, addressId);

            verify(customerAddressRepository).delete(testAddress);
        }

        @Test
        @DisplayName("should throw AddressNotFoundException when address not found")
        void shouldThrowWhenAddressNotFound() {
            when(customerProfileRepository.findByKeycloakUserId(KEYCLOAK_USER_ID))
                    .thenReturn(Optional.of(testProfile));
            when(customerAddressRepository.findByIdAndCustomerId(addressId, profileId))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    customerAddressService.deleteCustomerAddress(KEYCLOAK_USER_ID, addressId))
                    .isInstanceOf(AddressNotFoundException.class);

            verify(customerAddressRepository, never()).delete(any());
        }

        @Test
        @DisplayName("should throw CustomerNotFoundException when customer not found")
        void shouldThrowWhenCustomerNotFound() {
            when(customerProfileRepository.findByKeycloakUserId(KEYCLOAK_USER_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    customerAddressService.deleteCustomerAddress(KEYCLOAK_USER_ID, addressId))
                    .isInstanceOf(CustomerNotFoundException.class);
        }
    }

    // ──────────────────────────────────────────────────────────────
    //  setDefaultAddress
    // ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("setDefaultAddress")
    class SetDefaultAddress {

        @Test
        @DisplayName("should clear old defaults, set new default, and return response")
        void shouldSetDefaultAddress() {
            when(customerProfileRepository.findByKeycloakUserId(KEYCLOAK_USER_ID))
                    .thenReturn(Optional.of(testProfile));
            when(customerAddressRepository.findByIdAndCustomerId(addressId, profileId))
                    .thenReturn(Optional.of(testAddress));

            DefaultAddressResponse result =
                    customerAddressService.setDefaultAddress(KEYCLOAK_USER_ID, addressId);

            assertThat(result.getMessage()).isEqualTo("Default address updated successfully");
            assertThat(result.getActiveDefaultAddressId()).isEqualTo(addressId);
            assertThat(testAddress.isDefaultAddress()).isTrue();

            // Verify order: clear defaults first, then save new default
            var inOrder = inOrder(customerAddressRepository);
            inOrder.verify(customerAddressRepository).clearDefaultAddresses(profileId);
            inOrder.verify(customerAddressRepository).save(testAddress);
        }

        @Test
        @DisplayName("should throw AddressNotFoundException when address not found")
        void shouldThrowWhenAddressNotFound() {
            when(customerProfileRepository.findByKeycloakUserId(KEYCLOAK_USER_ID))
                    .thenReturn(Optional.of(testProfile));
            when(customerAddressRepository.findByIdAndCustomerId(addressId, profileId))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    customerAddressService.setDefaultAddress(KEYCLOAK_USER_ID, addressId))
                    .isInstanceOf(AddressNotFoundException.class);

            verify(customerAddressRepository, never()).clearDefaultAddresses(any());
        }

        @Test
        @DisplayName("should throw CustomerNotFoundException when customer not found")
        void shouldThrowWhenCustomerNotFound() {
            when(customerProfileRepository.findByKeycloakUserId(KEYCLOAK_USER_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    customerAddressService.setDefaultAddress(KEYCLOAK_USER_ID, addressId))
                    .isInstanceOf(CustomerNotFoundException.class);
        }
    }
}
