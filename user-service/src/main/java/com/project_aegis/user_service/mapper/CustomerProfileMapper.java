package com.project_aegis.user_service.mapper;

import com.project_aegis.user_service.dto.customer.request.CustomerUpdateRequest;
import com.project_aegis.user_service.dto.customer.response.CustomerProfileResponse;
import com.project_aegis.user_service.entity.CustomerProfile;
import lombok.Builder;
import lombok.Data;
import org.springframework.stereotype.Component;

@Component
@Data
@Builder
public class CustomerProfileMapper {

    public CustomerProfileResponse toResponse(CustomerProfile profile) {
        return CustomerProfileResponse.builder().id(profile.getId()).email(profile.getEmail()).firstName(profile.getFirstName()).lastName(profile.getLastName()).phoneNumber(profile.getPhoneNumber()).accountStatus(profile.getAccountStatus()).createdAt(profile.getCreatedAt()).updatedAt(profile.getUpdatedAt()).build();
    }

    /**
     * <p>Update {@link CustomerProfile}</p>
     * @param request update Request
     * @param profile Updating the  profile Object from DB
     * @return {@link CustomerProfile }
     */
    public CustomerProfile updateEntity(CustomerUpdateRequest request, CustomerProfile profile) {

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

        return profile;
    }
}
