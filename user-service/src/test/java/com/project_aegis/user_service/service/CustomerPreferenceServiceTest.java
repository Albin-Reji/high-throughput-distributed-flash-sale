package com.project_aegis.user_service.service;

import com.project_aegis.user_service.dto.customer.request.CustomerPreferenceRequest;
import com.project_aegis.user_service.dto.customer.response.CustomerPreferenceResponse;
import com.project_aegis.user_service.entity.CustomerPreference;
import com.project_aegis.user_service.entity.CustomerProfile;
import com.project_aegis.user_service.exception.CustomerNotFoundException;
import com.project_aegis.user_service.repository.CustomerProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CustomerPreferenceService Unit Tests")
class CustomerPreferenceServiceTest {

    @Mock
    private CustomerProfileRepository customerProfileRepository;

    @InjectMocks
    private CustomerPreferenceService customerPreferenceService;

    private static final String KEYCLOAK_USER_ID = "kc-user-001";

    private CustomerProfile testProfile;

    @BeforeEach
    void setUp() {
        UUID profileId = UUID.randomUUID();

        testProfile = CustomerProfile.builder()
                .id(profileId)
                .keycloakUserId(KEYCLOAK_USER_ID)
                .email("john@example.com")
                .firstName("John")
                .lastName("Doe")
                .build();
    }

    // ──────────────────────────────────────────────────────────────
    //  getPreferences
    // ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getPreferences")
    class GetPreferences {

        @Test
        @DisplayName("should return existing preferences when present")
        void shouldReturnExistingPreferences() {
            CustomerPreference preference = CustomerPreference.builder()
                    .customer(testProfile)
                    .marketingEmailsEnabled(true)
                    .smsNotificationsEnabled(true)
                    .preferredCurrency("USD")
                    .build();
            testProfile.setPreference(preference);

            when(customerProfileRepository.findByKeycloakUserId(KEYCLOAK_USER_ID))
                    .thenReturn(Optional.of(testProfile));

            CustomerPreferenceResponse result =
                    customerPreferenceService.getPreferences(KEYCLOAK_USER_ID);

            assertThat(result.isMarketingEmailsEnabled()).isTrue();
            assertThat(result.isSmsNotificationsEnabled()).isTrue();
            assertThat(result.getPreferredCurrency()).isEqualTo("USD");
        }

        @Test
        @DisplayName("should return default preferences when preference is null")
        void shouldReturnDefaultsWhenNull() {
            testProfile.setPreference(null);

            when(customerProfileRepository.findByKeycloakUserId(KEYCLOAK_USER_ID))
                    .thenReturn(Optional.of(testProfile));

            CustomerPreferenceResponse result =
                    customerPreferenceService.getPreferences(KEYCLOAK_USER_ID);

            assertThat(result.isMarketingEmailsEnabled()).isFalse();
            assertThat(result.isSmsNotificationsEnabled()).isFalse();
            assertThat(result.getPreferredCurrency()).isEqualTo("INR");
        }

        @Test
        @DisplayName("should throw CustomerNotFoundException when profile not found")
        void shouldThrowWhenNotFound() {
            when(customerProfileRepository.findByKeycloakUserId(KEYCLOAK_USER_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    customerPreferenceService.getPreferences(KEYCLOAK_USER_ID))
                    .isInstanceOf(CustomerNotFoundException.class)
                    .hasMessageContaining("Customer profile not found");
        }
    }

    // ──────────────────────────────────────────────────────────────
    //  replacePreferences (PUT — full replace)
    // ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("replacePreferences")
    class ReplacePreferences {

        @Test
        @DisplayName("should replace all fields on existing preference")
        void shouldReplaceExistingPreference() {
            CustomerPreference existingPref = CustomerPreference.builder()
                    .customer(testProfile)
                    .marketingEmailsEnabled(false)
                    .smsNotificationsEnabled(false)
                    .preferredCurrency("INR")
                    .build();
            testProfile.setPreference(existingPref);

            CustomerPreferenceRequest request = CustomerPreferenceRequest.builder()
                    .marketingEmailsEnabled(true)
                    .smsNotificationsEnabled(true)
                    .preferredCurrency("EUR")
                    .build();

            when(customerProfileRepository.findByKeycloakUserId(KEYCLOAK_USER_ID))
                    .thenReturn(Optional.of(testProfile));
            when(customerProfileRepository.save(testProfile))
                    .thenReturn(testProfile);

            CustomerPreferenceResponse result =
                    customerPreferenceService.replacePreferences(KEYCLOAK_USER_ID, request);

            assertThat(result.isMarketingEmailsEnabled()).isTrue();
            assertThat(result.isSmsNotificationsEnabled()).isTrue();
            assertThat(result.getPreferredCurrency()).isEqualTo("EUR");
            verify(customerProfileRepository).save(testProfile);
        }

        @Test
        @DisplayName("should create new preference when none exists")
        void shouldCreatePreferenceWhenNull() {
            testProfile.setPreference(null);

            CustomerPreferenceRequest request = CustomerPreferenceRequest.builder()
                    .marketingEmailsEnabled(true)
                    .smsNotificationsEnabled(false)
                    .preferredCurrency("GBP")
                    .build();

            when(customerProfileRepository.findByKeycloakUserId(KEYCLOAK_USER_ID))
                    .thenReturn(Optional.of(testProfile));
            when(customerProfileRepository.save(testProfile))
                    .thenReturn(testProfile);

            CustomerPreferenceResponse result =
                    customerPreferenceService.replacePreferences(KEYCLOAK_USER_ID, request);

            assertThat(result.isMarketingEmailsEnabled()).isTrue();
            assertThat(result.isSmsNotificationsEnabled()).isFalse();
            assertThat(result.getPreferredCurrency()).isEqualTo("GBP");
            // Verify preference was linked to profile
            assertThat(testProfile.getPreference()).isNotNull();
        }

        @Test
        @DisplayName("should use defaults for null request fields")
        void shouldUseDefaultsForNullFields() {
            CustomerPreference existingPref = CustomerPreference.builder()
                    .customer(testProfile)
                    .marketingEmailsEnabled(true)
                    .smsNotificationsEnabled(true)
                    .preferredCurrency("USD")
                    .build();
            testProfile.setPreference(existingPref);

            // All fields null — should preserve existing boolean values and default currency
            CustomerPreferenceRequest request = CustomerPreferenceRequest.builder().build();

            when(customerProfileRepository.findByKeycloakUserId(KEYCLOAK_USER_ID))
                    .thenReturn(Optional.of(testProfile));
            when(customerProfileRepository.save(testProfile))
                    .thenReturn(testProfile);

            CustomerPreferenceResponse result =
                    customerPreferenceService.replacePreferences(KEYCLOAK_USER_ID, request);

            // Booleans should keep existing values, currency defaults to "INR" when null
            assertThat(result.isMarketingEmailsEnabled()).isTrue();
            assertThat(result.isSmsNotificationsEnabled()).isTrue();
            assertThat(result.getPreferredCurrency()).isEqualTo("INR");
        }

        @Test
        @DisplayName("should throw CustomerNotFoundException when profile not found")
        void shouldThrowWhenNotFound() {
            CustomerPreferenceRequest request = CustomerPreferenceRequest.builder().build();

            when(customerProfileRepository.findByKeycloakUserId(KEYCLOAK_USER_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    customerPreferenceService.replacePreferences(KEYCLOAK_USER_ID, request))
                    .isInstanceOf(CustomerNotFoundException.class);
        }
    }

    // ──────────────────────────────────────────────────────────────
    //  updatePreferences (PATCH — partial update)
    // ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("updatePreferences")
    class UpdatePreferences {

        @Test
        @DisplayName("should only update non-null fields on existing preference")
        void shouldPartiallyUpdate() {
            CustomerPreference existingPref = CustomerPreference.builder()
                    .customer(testProfile)
                    .marketingEmailsEnabled(false)
                    .smsNotificationsEnabled(false)
                    .preferredCurrency("INR")
                    .build();
            testProfile.setPreference(existingPref);

            // Only update marketingEmailsEnabled
            CustomerPreferenceRequest request = CustomerPreferenceRequest.builder()
                    .marketingEmailsEnabled(true)
                    .build();

            when(customerProfileRepository.findByKeycloakUserId(KEYCLOAK_USER_ID))
                    .thenReturn(Optional.of(testProfile));
            when(customerProfileRepository.save(testProfile))
                    .thenReturn(testProfile);

            CustomerPreferenceResponse result =
                    customerPreferenceService.updatePreferences(KEYCLOAK_USER_ID, request);

            assertThat(result.isMarketingEmailsEnabled()).isTrue();           // updated
            assertThat(result.isSmsNotificationsEnabled()).isFalse();         // unchanged
            assertThat(result.getPreferredCurrency()).isEqualTo("INR");       // unchanged
        }

        @Test
        @DisplayName("should update only smsNotificationsEnabled when others are null")
        void shouldUpdateOnlySms() {
            CustomerPreference existingPref = CustomerPreference.builder()
                    .customer(testProfile)
                    .marketingEmailsEnabled(true)
                    .smsNotificationsEnabled(false)
                    .preferredCurrency("USD")
                    .build();
            testProfile.setPreference(existingPref);

            CustomerPreferenceRequest request = CustomerPreferenceRequest.builder()
                    .smsNotificationsEnabled(true)
                    .build();

            when(customerProfileRepository.findByKeycloakUserId(KEYCLOAK_USER_ID))
                    .thenReturn(Optional.of(testProfile));
            when(customerProfileRepository.save(testProfile))
                    .thenReturn(testProfile);

            CustomerPreferenceResponse result =
                    customerPreferenceService.updatePreferences(KEYCLOAK_USER_ID, request);

            assertThat(result.isMarketingEmailsEnabled()).isTrue();           // unchanged
            assertThat(result.isSmsNotificationsEnabled()).isTrue();          // updated
            assertThat(result.getPreferredCurrency()).isEqualTo("USD");       // unchanged
        }

        @Test
        @DisplayName("should update only preferredCurrency when others are null")
        void shouldUpdateOnlyCurrency() {
            CustomerPreference existingPref = CustomerPreference.builder()
                    .customer(testProfile)
                    .marketingEmailsEnabled(false)
                    .smsNotificationsEnabled(true)
                    .preferredCurrency("INR")
                    .build();
            testProfile.setPreference(existingPref);

            CustomerPreferenceRequest request = CustomerPreferenceRequest.builder()
                    .preferredCurrency("JPY")
                    .build();

            when(customerProfileRepository.findByKeycloakUserId(KEYCLOAK_USER_ID))
                    .thenReturn(Optional.of(testProfile));
            when(customerProfileRepository.save(testProfile))
                    .thenReturn(testProfile);

            CustomerPreferenceResponse result =
                    customerPreferenceService.updatePreferences(KEYCLOAK_USER_ID, request);

            assertThat(result.isMarketingEmailsEnabled()).isFalse();          // unchanged
            assertThat(result.isSmsNotificationsEnabled()).isTrue();          // unchanged
            assertThat(result.getPreferredCurrency()).isEqualTo("JPY");       // updated
        }

        @Test
        @DisplayName("should create new preference when none exists")
        void shouldCreatePreferenceWhenNull() {
            testProfile.setPreference(null);

            CustomerPreferenceRequest request = CustomerPreferenceRequest.builder()
                    .marketingEmailsEnabled(true)
                    .build();

            when(customerProfileRepository.findByKeycloakUserId(KEYCLOAK_USER_ID))
                    .thenReturn(Optional.of(testProfile));
            when(customerProfileRepository.save(testProfile))
                    .thenReturn(testProfile);

            CustomerPreferenceResponse result =
                    customerPreferenceService.updatePreferences(KEYCLOAK_USER_ID, request);

            assertThat(testProfile.getPreference()).isNotNull();
            assertThat(result.isMarketingEmailsEnabled()).isTrue();
        }

        @Test
        @DisplayName("should not modify any fields when all request fields are null")
        void shouldNotModifyWhenAllNull() {
            CustomerPreference existingPref = CustomerPreference.builder()
                    .customer(testProfile)
                    .marketingEmailsEnabled(true)
                    .smsNotificationsEnabled(true)
                    .preferredCurrency("EUR")
                    .build();
            testProfile.setPreference(existingPref);

            CustomerPreferenceRequest request = CustomerPreferenceRequest.builder().build();

            when(customerProfileRepository.findByKeycloakUserId(KEYCLOAK_USER_ID))
                    .thenReturn(Optional.of(testProfile));
            when(customerProfileRepository.save(testProfile))
                    .thenReturn(testProfile);

            CustomerPreferenceResponse result =
                    customerPreferenceService.updatePreferences(KEYCLOAK_USER_ID, request);

            assertThat(result.isMarketingEmailsEnabled()).isTrue();
            assertThat(result.isSmsNotificationsEnabled()).isTrue();
            assertThat(result.getPreferredCurrency()).isEqualTo("EUR");
        }

        @Test
        @DisplayName("should throw CustomerNotFoundException when profile not found")
        void shouldThrowWhenNotFound() {
            CustomerPreferenceRequest request = CustomerPreferenceRequest.builder().build();

            when(customerProfileRepository.findByKeycloakUserId(KEYCLOAK_USER_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    customerPreferenceService.updatePreferences(KEYCLOAK_USER_ID, request))
                    .isInstanceOf(CustomerNotFoundException.class);
        }
    }
}
