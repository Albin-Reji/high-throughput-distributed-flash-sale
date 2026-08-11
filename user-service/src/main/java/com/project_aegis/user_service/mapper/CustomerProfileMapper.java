package com.project_aegis.user_service.mapper;

import com.project_aegis.user_service.dto.customer.CustomerProfileResponse;
import com.project_aegis.user_service.entity.CustomerProfile;
import lombok.Builder;
import lombok.Data;
import org.springframework.stereotype.Component;

@Component
@Data
@Builder
public class CustomerProfileMapper {

    public CustomerProfileResponse toResponse(CustomerProfile  profile){
        return CustomerProfileResponse.builder()
                .id(profile.getId())
                .email(profile.getEmail())
                .firstName(profile.getFirstName())
                .lastName(profile.getLastName())
                .phoneNumber(profile.getPhoneNumber())
                .accountStatus(profile.getAccountStatus())
                .createdAt(profile.getCreatedAt())
                .updatedAt(profile.getUpdatedAt())
                .build();
    }
}
