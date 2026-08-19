package com.project_aegis.user_service.dto.response;

import com.project_aegis.user_service.entity.CustomerProfile;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProfileCreationResult {
    private CustomerProfile profile;
    private boolean created;
}
