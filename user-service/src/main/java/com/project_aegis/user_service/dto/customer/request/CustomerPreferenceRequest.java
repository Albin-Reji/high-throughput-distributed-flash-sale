package com.project_aegis.user_service.dto.customer.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CustomerPreferenceRequest {

    private Boolean marketingEmailsEnabled;
    private Boolean smsNotificationsEnabled;
    private String preferredCurrency;
}
