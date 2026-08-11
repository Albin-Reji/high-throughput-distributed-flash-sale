package com.project_aegis.user_service.service.impl;

import com.project_aegis.user_service.dto.webhook.KeycloakUserRegisteredEvent;
import com.project_aegis.user_service.entity.AccountStatus;
import com.project_aegis.user_service.entity.CustomerPreference;
import com.project_aegis.user_service.entity.CustomerProfile;
import com.project_aegis.user_service.repository.CustomerProfileRepository;
import com.project_aegis.user_service.service.CustomerProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerProfileServiceImpl implements CustomerProfileService {

    private final CustomerProfileRepository customerProfileRepository;

    /**
     * Creates a new {@link CustomerProfile} from a Keycloak registration event,
     * or returns the existing one if a profile with the same keycloakUserId
     * already exists (idempotent).
     */
    @Override
    @Transactional
    public CustomerProfile createProfileFromKeycloakEvent(KeycloakUserRegisteredEvent event) {

        // Idempotency check — if the profile already exists, return it
        Optional<CustomerProfile> existing =
                customerProfileRepository.findByKeycloakUserId(event.keycloakUserId());

        if (existing.isPresent()) {
            log.info("CustomerProfile already exists for keycloakUserId={}, skipping creation",
                    event.keycloakUserId());
            return existing.get();
        }

        // Build the new profile
        CustomerProfile profile = CustomerProfile.builder()
                .keycloakUserId(event.keycloakUserId())
                .email(event.email())
                .firstName(event.firstName())
                .lastName(event.lastName())
                .accountStatus(AccountStatus.ACTIVE)
                .build();

        // Build default preferences and link them
        CustomerPreference preference = CustomerPreference.builder()
                .customer(profile)
                .marketingEmailsEnabled(false)
                .smsNotificationsEnabled(false)
                .preferredCurrency("INR")
                .build();

        profile.setPreference(preference);

        CustomerProfile saved = customerProfileRepository.save(profile);

        log.info("CustomerProfile created: id={}, keycloakUserId={}, email={}",
                saved.getId(), saved.getKeycloakUserId(), saved.getEmail());

        return saved;
    }
}
