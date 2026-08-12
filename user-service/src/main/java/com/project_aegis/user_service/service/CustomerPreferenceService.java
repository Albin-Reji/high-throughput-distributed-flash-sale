package com.project_aegis.user_service.service;

import com.project_aegis.user_service.dto.customer.request.CustomerPreferenceRequest;
import com.project_aegis.user_service.dto.customer.response.CustomerPreferenceResponse;

public interface CustomerPreferenceService {

    CustomerPreferenceResponse getPreferences(String keycloakUserId);

    CustomerPreferenceResponse replacePreferences(String keycloakUserId, CustomerPreferenceRequest request);

    CustomerPreferenceResponse updatePreferences(String keycloakUserId, CustomerPreferenceRequest request);
}
