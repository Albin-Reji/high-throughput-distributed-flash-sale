package com.project_aegis.user_service.dto.request;

import com.project_aegis.user_service.entity.AccountStatus;
import lombok.Data;

@Data
public class StatusRequest {
    private AccountStatus accountStatus;
    private String reason;
}
