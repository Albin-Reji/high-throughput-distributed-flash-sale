package com.project_aegis.keycloak.event;

import org.keycloak.Config;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.models.KeycloakSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserRegistrationEventListenerProviderFactory Unit Tests")
class UserRegistrationEventListenerProviderFactoryTest {

    @Mock
    private KeycloakSession session;

    @Mock
    private Config.Scope config;

    private UserRegistrationEventListenerProviderFactory factory;

    @BeforeEach
    void setUp() {
        factory = new UserRegistrationEventListenerProviderFactory();
    }

    @Test
    @DisplayName("getId should return 'user-registration-sync'")
    void getIdShouldReturnExpectedValue() {
        assertThat(factory.getId()).isEqualTo("user-registration-sync");
    }

    @Test
    @DisplayName("create should return a UserRegistrationEventListenerProvider instance")
    void createShouldReturnProviderInstance() {
        // Initialize factory first
        when(config.get("webhookUrl", "http://localhost:8081/internal/webhook/keycloak/user-registered"))
                .thenReturn("http://test-url/webhook");
        when(config.get("apiKey", "")).thenReturn("test-key");
        factory.init(config);

        EventListenerProvider provider = factory.create(session);

        assertThat(provider).isNotNull();
        assertThat(provider).isInstanceOf(UserRegistrationEventListenerProvider.class);
    }

    @Test
    @DisplayName("init should use default webhook URL when not configured")
    void initShouldUseDefaults() {
        when(config.get("webhookUrl", "http://localhost:8081/internal/webhook/keycloak/user-registered"))
                .thenReturn("http://localhost:8081/internal/webhook/keycloak/user-registered");
        when(config.get("apiKey", "")).thenReturn("");

        factory.init(config);

        // Should not throw — blank API key triggers a log warning but continues
        EventListenerProvider provider = factory.create(session);
        assertThat(provider).isNotNull();
    }

    @Test
    @DisplayName("postInit should not throw")
    void postInitShouldNotThrow() {
        factory.postInit(null);
        // No exception means success
    }

    @Test
    @DisplayName("close should not throw")
    void closeShouldNotThrow() {
        factory.close();
        // No exception means success
    }
}
