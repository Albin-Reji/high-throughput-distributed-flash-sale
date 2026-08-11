package com.project_aegis.user_service.dto.address.response;

import com.project_aegis.user_service.entity.AddressType;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CustomerAddressResponse {

    private AddressType addressType;
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String state;
    private String postalCode;
    private String country;
    private boolean defaultAddress;
}
