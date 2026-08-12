package com.project_aegis.user_service.service;

import com.project_aegis.user_service.dto.address.response.CustomerAddressResponse;
import com.project_aegis.user_service.dto.address.response.DefaultAddressResponse;
import com.project_aegis.user_service.dto.customer.request.CustomerAddressRequest;
import com.project_aegis.user_service.dto.response.ApiResponse;

import java.util.List;
import java.util.UUID;

public interface CustomerAddressService {
    ApiResponse<CustomerAddressResponse> addCustomerAddress(String keycloakUserId, CustomerAddressRequest customerAddressRequest);

    ApiResponse<List<CustomerAddressResponse>> getCustomerAddress(String keycloakUserId);

    ApiResponse<CustomerAddressResponse> getCustomerAddressByCustomerId(String keycloakUserId, UUID addressId);

    ApiResponse<CustomerAddressResponse> updateCustomerAddress(String keycloakUserId, UUID addressId, CustomerAddressRequest customerAddressRequest);

    void deleteCustomerAddress(String keycloakUserId, UUID addressId);

    DefaultAddressResponse setDefaultAddress(String keycloakUserId, UUID addressId);
}

