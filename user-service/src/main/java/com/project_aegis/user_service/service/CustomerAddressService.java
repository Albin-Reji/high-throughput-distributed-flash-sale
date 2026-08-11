package com.project_aegis.user_service.service;

import com.project_aegis.user_service.dto.address.response.CustomerAddressResponse;
import com.project_aegis.user_service.dto.customer.request.CustomerAddressRequest;
import com.project_aegis.user_service.dto.response.ApiResponse;

public interface CustomerAddressService {
    ApiResponse<CustomerAddressResponse> addCustomerAddress(String keycloakUserId, CustomerAddressRequest customerAddressRequest);
}
