package com.project_aegis.user_service.dto.address.response;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class DefaultAddressResponse {
    private String message;
    private UUID activeDefaultAddressId;
}
