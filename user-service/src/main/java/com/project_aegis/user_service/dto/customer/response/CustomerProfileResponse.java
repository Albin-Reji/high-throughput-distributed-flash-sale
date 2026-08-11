package com.project_aegis.user_service.dto.customer.response;

import com.project_aegis.user_service.entity.AccountStatus;
import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
public record CustomerProfileResponse(
        UUID id,
        String email,
        String firstName,
        String lastName,
        String phoneNumber,
        AccountStatus accountStatus,
        Instant createdAt,
        Instant updatedAt
) {
}
