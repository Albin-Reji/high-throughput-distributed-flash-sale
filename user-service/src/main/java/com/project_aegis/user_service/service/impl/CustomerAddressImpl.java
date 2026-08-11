package com.project_aegis.user_service.service.impl;

import com.project_aegis.user_service.dto.address.response.CustomerAddressResponse;
import com.project_aegis.user_service.dto.customer.request.CustomerAddressRequest;
import com.project_aegis.user_service.dto.response.ApiResponse;
import com.project_aegis.user_service.entity.CustomerAddress;
import com.project_aegis.user_service.entity.CustomerProfile;
import com.project_aegis.user_service.exception.CustomerNotFoundException;
import com.project_aegis.user_service.mapper.CustomerAddressMapper;
import com.project_aegis.user_service.repository.CustomerAddressRepository;
import com.project_aegis.user_service.repository.CustomerProfileRepository;
import com.project_aegis.user_service.service.CustomerAddressService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomerAddressImpl implements CustomerAddressService {

    private final CustomerAddressRepository customerAddressRepository;
    private final CustomerProfileRepository customerProfileRepository;
    private final CustomerAddressMapper customerAddressMapper;

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
                .orElseThrow(() -> new CustomerNotFoundException("Customer profile not found "));

        CustomerAddress customerAddress = customerAddressMapper.toEntity(customerAddressRequest);
        customerAddress.setCustomer(customerProfile);

        CustomerAddress savedAddress = customerAddressRepository.save(customerAddress);
        return ApiResponse.<CustomerAddressResponse>builder()
                .success(true)
                .message("Customer address added successfully")
                .data(customerAddressMapper.toResponse(savedAddress))
                .build();


    }
}
