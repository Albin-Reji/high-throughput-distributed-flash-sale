package com.project_aegis.user_service.dto.response;

import com.project_aegis.user_service.dto.address.response.CustomerAddressResponse;
import com.project_aegis.user_service.dto.customer.response.CustomerPreferenceResponse;
import com.project_aegis.user_service.entity.AccountStatus;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class ProfileResponse {
    private UUID id;
    private String email;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private AccountStatus accountStatus;
    private Instant createdAt;
    private Instant updatedAt;
    private CustomerPreferenceResponse preference;
    private List<CustomerAddressResponse> addresses;
}
