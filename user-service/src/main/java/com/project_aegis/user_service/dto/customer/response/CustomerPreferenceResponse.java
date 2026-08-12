package com.project_aegis.user_service.dto.customer.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CustomerPreferenceResponse {
    private boolean marketingEmailsEnabled;
    private boolean smsNotificationsEnabled;
    private String preferredCurrency;
}
