package com.project_aegis.user_service.mapper;

import com.project_aegis.user_service.dto.address.response.CustomerAddressResponse;
import com.project_aegis.user_service.dto.customer.request.CustomerAddressRequest;
import com.project_aegis.user_service.entity.CustomerAddress;
import lombok.AllArgsConstructor;
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
}
