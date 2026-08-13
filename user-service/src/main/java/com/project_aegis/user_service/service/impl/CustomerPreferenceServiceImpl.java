package com.project_aegis.user_service.service.impl;

import com.project_aegis.user_service.dto.customer.request.CustomerPreferenceRequest;
import com.project_aegis.user_service.dto.customer.response.CustomerPreferenceResponse;
import com.project_aegis.user_service.entity.CustomerPreference;
import com.project_aegis.user_service.entity.CustomerProfile;
import com.project_aegis.user_service.exception.CustomerNotFoundException;
import com.project_aegis.user_service.repository.CustomerProfileRepository;
import com.project_aegis.user_service.service.CustomerPreferenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerPreferenceServiceImpl implements CustomerPreferenceService {

    private final CustomerProfileRepository customerProfileRepository;

    private static final String CUSTOMER_PROFILE_NOT_FOUND="Customer profile not found ";

    /**
     * <p>Get customer preferences</p>
     */
    @Override
    @Transactional(readOnly = true)
    public CustomerPreferenceResponse getPreferences(String keycloakUserId) {
        CustomerProfile profile = customerProfileRepository.findByKeycloakUserId(keycloakUserId)
                .orElseThrow(() -> new CustomerNotFoundException(CUSTOMER_PROFILE_NOT_FOUND));

        CustomerPreference preference = profile.getPreference();
        if (preference == null) {
            // Return defaults if no preference record exists
            return CustomerPreferenceResponse.builder()
                    .marketingEmailsEnabled(false)
                    .smsNotificationsEnabled(false)
                    .preferredCurrency("INR")
                    .build();
        }

        return toResponse(preference);
    }

    /**
     * <p>Full replace of customer preferences (PUT)</p>
     */
    @Override
    @Transactional
    public CustomerPreferenceResponse replacePreferences(String keycloakUserId,
                                                          CustomerPreferenceRequest request) {
        CustomerProfile profile = customerProfileRepository.findByKeycloakUserId(keycloakUserId)
                .orElseThrow(() -> new CustomerNotFoundException(CUSTOMER_PROFILE_NOT_FOUND));

        CustomerPreference preference = profile.getPreference();
        if (preference == null) {
            preference = CustomerPreference.builder()
                    .customer(profile)
                    .build();
            profile.setPreference(preference);
        }

        // Full replace — set all fields
        preference.setMarketingEmailsEnabled(request.getMarketingEmailsEnabled()!=null?request.getMarketingEmailsEnabled(): preference.isMarketingEmailsEnabled());
        preference.setSmsNotificationsEnabled(request.getSmsNotificationsEnabled()!=null?request.getSmsNotificationsEnabled(): preference.isSmsNotificationsEnabled());
        preference.setPreferredCurrency(
                request.getPreferredCurrency() != null ? request.getPreferredCurrency() : "INR");

        customerProfileRepository.save(profile);
        log.info("Preferences fully replaced for keycloakUserId={}", keycloakUserId);

        return toResponse(preference);
    }

    /**
     * <p>Partial update of customer preferences (PATCH)</p>
     */
    @Override
    @Transactional
    public CustomerPreferenceResponse updatePreferences(String keycloakUserId,
                                                         CustomerPreferenceRequest request) {
        CustomerProfile profile = customerProfileRepository.findByKeycloakUserId(keycloakUserId)
                .orElseThrow(() -> new CustomerNotFoundException(CUSTOMER_PROFILE_NOT_FOUND));

        CustomerPreference preference = profile.getPreference();
        if (preference == null) {
            preference = CustomerPreference.builder()
                    .customer(profile)
                    .build();
            profile.setPreference(preference);
        }

        // Partial update — only set non-null fields
        if (request.getMarketingEmailsEnabled() != null) {
            preference.setMarketingEmailsEnabled(request.getMarketingEmailsEnabled());
        }
        if (request.getSmsNotificationsEnabled() != null) {
            preference.setSmsNotificationsEnabled(request.getSmsNotificationsEnabled());
        }
        if (request.getPreferredCurrency() != null) {
            preference.setPreferredCurrency(request.getPreferredCurrency());
        }

        customerProfileRepository.save(profile);
        log.info("Preferences partially updated for keycloakUserId={}", keycloakUserId);

        return toResponse(preference);
    }

    private CustomerPreferenceResponse toResponse(CustomerPreference preference) {
        return CustomerPreferenceResponse.builder()
                .marketingEmailsEnabled(preference.isMarketingEmailsEnabled())
                .smsNotificationsEnabled(preference.isSmsNotificationsEnabled())
                .preferredCurrency(preference.getPreferredCurrency())
                .build();
    }
}
