package com.project_aegis.user_service.service.impl;

import com.project_aegis.user_service.dto.address.response.CustomerAddressResponse;
import com.project_aegis.user_service.dto.address.response.DefaultAddressResponse;
import com.project_aegis.user_service.dto.customer.request.CustomerAddressRequest;
import com.project_aegis.user_service.dto.response.ApiResponse;
import com.project_aegis.user_service.entity.CustomerAddress;
import com.project_aegis.user_service.entity.CustomerProfile;
import com.project_aegis.user_service.exception.AddressNotFoundException;
import com.project_aegis.user_service.exception.CustomerNotFoundException;
import com.project_aegis.user_service.mapper.CustomerAddressMapper;
import com.project_aegis.user_service.repository.CustomerAddressRepository;
import com.project_aegis.user_service.repository.CustomerProfileRepository;
import com.project_aegis.user_service.service.CustomerAddressService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerAddressServiceImpl implements CustomerAddressService {

    private final CustomerAddressRepository customerAddressRepository;
    private final CustomerProfileRepository customerProfileRepository;
    private final CustomerAddressMapper customerAddressMapper;

    private static final String ADDRESS_NOT_FOUND="Address not found with id:";
    private static final String CUSTOMER_NOT_FOUND="Customer not found with id:";
    /**
     * <p>Saved Address to DB</p>
     *
     * @param keycloakUserId         userID
     * @param customerAddressRequest customer request Structure
     * @return {@link ApiResponse} containing {@link CustomerAddressResponse}
     */
    @Override
    @Transactional
    public ApiResponse<CustomerAddressResponse> addCustomerAddress(String keycloakUserId,
                                                                   CustomerAddressRequest customerAddressRequest) {

        CustomerProfile customerProfile = customerProfileRepository.findByKeycloakUserId(keycloakUserId)
                .orElseThrow(() -> new CustomerNotFoundException(CUSTOMER_NOT_FOUND));

        CustomerAddress customerAddress = customerAddressMapper.toEntity(customerAddressRequest);
        customerAddress.setCustomer(customerProfile);

        CustomerAddress savedAddress = customerAddressRepository.save(customerAddress);
        return ApiResponse.<CustomerAddressResponse>builder()
                .success(true)
                .message("Customer address added successfully")
                .data(customerAddressMapper.toResponse(savedAddress))
                .build();


    }

    @Override
    public ApiResponse<List<CustomerAddressResponse>> getCustomerAddress(String keycloakUserId) {
        CustomerProfile profile = customerProfileRepository.findByKeycloakUserId(keycloakUserId)
                .orElseThrow(() -> new CustomerNotFoundException(CUSTOMER_NOT_FOUND));

        List<CustomerAddressResponse> addressResponses = customerAddressRepository.findAllByCustomerId(profile.getId())
                .stream()
                .map(customerAddressMapper::toResponse)
                .collect(Collectors.toCollection(ArrayList::new));
        if (addressResponses.isEmpty()) {
            throw new AddressNotFoundException("No address  found for this user");
        }

        return ApiResponse.<List<CustomerAddressResponse>>builder()
                .message("Customer address retrieved successfully")
                .success(true)
                .data(addressResponses)
                .build();
    }

    @Transactional
    @Override
    public ApiResponse<CustomerAddressResponse> getCustomerAddressByCustomerId(String keycloakUserId, UUID addressId) {
        CustomerProfile profile = customerProfileRepository.findByKeycloakUserId(keycloakUserId)
                .orElseThrow(() -> new CustomerNotFoundException(CUSTOMER_NOT_FOUND));

        CustomerAddress address = customerAddressRepository.findById(addressId)
                .orElseThrow(() -> new AddressNotFoundException(ADDRESS_NOT_FOUND+" " + addressId));

        //        Is this address belong to this user
        //        if yes then return
        //        else AddressNotFoundException

        if (!profile.getId().equals(address.getCustomer().getId())) {
            throw new AddressNotFoundException(
                    ADDRESS_NOT_FOUND + addressId
            );
        }

        return ApiResponse.<CustomerAddressResponse>builder()
                .success(true)
                .message("Address Found")
                .data(customerAddressMapper.toResponse(address))
                .build();
    }

    /**
     * <p>Update an existing address (full replace)</p>
     */
    @Override
    @Transactional
    public ApiResponse<CustomerAddressResponse> updateCustomerAddress(String keycloakUserId,
                                                                      UUID addressId,
                                                                      CustomerAddressRequest customerAddressRequest) {
        CustomerProfile profile = customerProfileRepository.findByKeycloakUserId(keycloakUserId)
                .orElseThrow(() -> new CustomerNotFoundException(CUSTOMER_NOT_FOUND));

        CustomerAddress address = customerAddressRepository.findByIdAndCustomerId(addressId, profile.getId())
                .orElseThrow(() -> new AddressNotFoundException(ADDRESS_NOT_FOUND + addressId));

        customerAddressMapper.updateEntity(customerAddressRequest, address);

        CustomerAddress savedAddress = customerAddressRepository.save(address);
        log.info("Address updated: addressId={}, customerId={}", addressId, profile.getId());

        return ApiResponse.<CustomerAddressResponse>builder()
                .success(true)
                .message("Customer address updated successfully")
                .data(customerAddressMapper.toResponse(savedAddress))
                .build();
    }

    /**
     * <p>Delete an address</p>
     */
    @Override
    @Transactional
    public void deleteCustomerAddress(String keycloakUserId, UUID addressId) {
        CustomerProfile profile = customerProfileRepository.findByKeycloakUserId(keycloakUserId)
                .orElseThrow(() -> new CustomerNotFoundException(CUSTOMER_NOT_FOUND));

        CustomerAddress address = customerAddressRepository.findByIdAndCustomerId(addressId, profile.getId())
                .orElseThrow(() -> new AddressNotFoundException(ADDRESS_NOT_FOUND + addressId));

        customerAddressRepository.delete(address);
        log.info("Address deleted: addressId={}, customerId={}", addressId, profile.getId());
    }

    /**
     * <p>Set an address as default, clearing any previously set default</p>
     */
    @Override
    @Transactional
    public DefaultAddressResponse setDefaultAddress(String keycloakUserId, UUID addressId) {
        CustomerProfile profile = customerProfileRepository.findByKeycloakUserId(keycloakUserId)
                .orElseThrow(() -> new CustomerNotFoundException(CUSTOMER_NOT_FOUND));

        CustomerAddress address = customerAddressRepository.findByIdAndCustomerId(addressId, profile.getId())
                .orElseThrow(() -> new AddressNotFoundException(ADDRESS_NOT_FOUND + addressId));

        // Clear all existing defaults for this customer
        customerAddressRepository.clearDefaultAddresses(profile.getId());

        // Set the new default
        address.setDefaultAddress(true);
        customerAddressRepository.save(address);

        log.info("Default address set: addressId={}, customerId={}", addressId, profile.getId());

        return DefaultAddressResponse.builder()
                .message("Default address updated successfully")
                .activeDefaultAddressId(address.getId())
                .build();
    }
}

