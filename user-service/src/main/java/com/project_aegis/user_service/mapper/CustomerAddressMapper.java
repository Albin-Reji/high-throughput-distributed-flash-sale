package com.project_aegis.user_service.mapper;

import com.project_aegis.user_service.dto.address.response.CustomerAddressResponse;
import com.project_aegis.user_service.dto.customer.request.CustomerAddressRequest;
import com.project_aegis.user_service.entity.CustomerAddress;
import lombok.Builder;
import lombok.Data;
import org.springframework.stereotype.Component;

@Data
@Builder
@Component
public class CustomerAddressMapper {

    public CustomerAddress toEntity(CustomerAddressRequest request) {
        return CustomerAddress.builder()
                .addressType(request.getAddressType())
                .addressLine1(request.getAddressLine1())
                .addressLine2(request.getAddressLine2())
                .state(request.getState())
                .city(request.getCity())
                .postalCode(request.getPostalCode())
                .country(request.getCountry())
                .defaultAddress(request.isDefaultAddress())
                .build();
    }

    public CustomerAddressResponse toResponse(CustomerAddress savedAddress) {
        return CustomerAddressResponse.builder()
                .id(savedAddress.getId())
                .addressType(savedAddress.getAddressType())
                .addressLine1(savedAddress.getAddressLine1())
                .addressLine2(savedAddress.getAddressLine2())
                .city(savedAddress.getCity())
                .state(savedAddress.getState())
                .postalCode(savedAddress.getPostalCode())
                .country(savedAddress.getCountry())
                .defaultAddress(savedAddress.isDefaultAddress())
                .build();
    }

    /**
     * Updates all fields on the existing entity from the request (full replace).
     */
    public void updateEntity(CustomerAddressRequest request, CustomerAddress entity) {
        entity.setAddressType(request.getAddressType());
        entity.setAddressLine1(request.getAddressLine1());
        entity.setAddressLine2(request.getAddressLine2());
        entity.setCity(request.getCity());
        entity.setState(request.getState());
        entity.setPostalCode(request.getPostalCode());
        entity.setCountry(request.getCountry());
        entity.setDefaultAddress(request.isDefaultAddress());
    }
}
