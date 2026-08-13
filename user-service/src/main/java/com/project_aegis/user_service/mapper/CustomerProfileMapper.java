package com.project_aegis.user_service.mapper;

import com.project_aegis.user_service.dto.customer.request.CustomerUpdateRequest;
import com.project_aegis.user_service.dto.customer.response.CustomerPreferenceResponse;
import com.project_aegis.user_service.dto.customer.response.CustomerProfileResponse;
import com.project_aegis.user_service.dto.response.ProfileResponse;
import com.project_aegis.user_service.entity.CustomerProfile;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.stream.Collectors;

@Component
@Data
@Builder
@RequiredArgsConstructor
public class CustomerProfileMapper {

    private final CustomerAddressMapper customerAddressMapper;

    public CustomerProfileResponse toResponse(CustomerProfile profile) {
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

    /**
     * <p>Update {@link CustomerProfile}</p>
     *
     * @param request update Request
     * @param profile Updating the  profile Object from DB
     */
    public void updateEntity(CustomerUpdateRequest request, CustomerProfile profile) {

        if (request.getEmail() != null) {
            profile.setEmail(request.getEmail());
        }
        if (request.getFirstName() != null) {
            profile.setFirstName(request.getFirstName());
        }

        if (request.getLastName() != null) {
            profile.setLastName(request.getLastName());
        }

        if (request.getPhoneNumber() != null) {
            profile.setPhoneNumber(request.getPhoneNumber());
        }

    }

    public void replaceEntity(
            CustomerUpdateRequest request,
            CustomerProfile profile) {

        profile.setEmail(request.getEmail());
        profile.setFirstName(request.getFirstName());
        profile.setLastName(request.getLastName());
        profile.setPhoneNumber(request.getPhoneNumber());

    }

    public ProfileResponse toProfileResponse(CustomerProfile profile) {
        return ProfileResponse.builder()
                .id(profile.getId())
                .email(profile.getEmail())
                .firstName(profile.getFirstName())
                .lastName(profile.getLastName())
                .phoneNumber(profile.getPhoneNumber())
                .accountStatus(profile.getAccountStatus())
                .createdAt(profile.getCreatedAt())
                .updatedAt(profile.getUpdatedAt())
                .preference(CustomerPreferenceResponse.builder()
                        .marketingEmailsEnabled(profile.getPreference()
                                .isMarketingEmailsEnabled())
                        .smsNotificationsEnabled(profile.getPreference()
                                .isSmsNotificationsEnabled())
                        .preferredCurrency(profile.getPreference()
                                .getPreferredCurrency())
                        .build()
                )
                .addresses(profile.getAddresses()
                        .stream()
                        .map(customerAddressMapper::toResponse)
                        .collect(Collectors.toCollection(ArrayList::new))
                )
                .build();
    }
}
