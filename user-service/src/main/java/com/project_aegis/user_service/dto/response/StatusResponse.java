package com.project_aegis.user_service.dto.response;

import com.project_aegis.user_service.entity.AccountStatus;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class StatusResponse {
    private UUID   customerId;
    private AccountStatus accountStatus;
    @Builder.Default
    private Instant updatedAt=Instant.now();
}
