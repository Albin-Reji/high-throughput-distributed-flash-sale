package com.project_aegis.keycloak.event;

import org.keycloak.Config;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.logging.Logger;

/**
 * Factory for {@link UserRegistrationEventListenerProvider}.
 *
 * <h3>Keycloak Configuration</h3>
 * <p>Add the following to your Keycloak server configuration
 * (e.g., environment variables for the Quarkus distribution):</p>
 * <pre>
 *   KC_SPI_EVENTS_LISTENER_USER_REGISTRATION_SYNC_WEBHOOK_URL=http://user-service:8081/internal/webhook/keycloak/user-registered
 *   KC_SPI_EVENTS_LISTENER_USER_REGISTRATION_SYNC_API_KEY=your-secret-api-key
 * </pre>
 *
 * <p>Or in {@code keycloak.conf}:</p>
 * <pre>
 *   spi-events-listener-user-registration-sync-webhook-url=http://user-service:8081/internal/webhook/keycloak/user-registered
 *   spi-events-listener-user-registration-sync-api-key=your-secret-api-key
 * </pre>
 *
 * <p>Then enable the event listener in your realm's Events settings
 * by adding {@code "user-registration-sync"} to the listeners list.</p>
 */
public class UserRegistrationEventListenerProviderFactory implements EventListenerProviderFactory {

    private static final Logger LOG =
            Logger.getLogger(UserRegistrationEventListenerProviderFactory.class.getName());

    public static final String PROVIDER_ID = "user-registration-sync";

    private HttpClient httpClient;
    private String webhookUrl;
    private String apiKey;

    @Override
    public EventListenerProvider create(KeycloakSession session) {
        return new UserRegistrationEventListenerProvider(session, httpClient, webhookUrl, apiKey);
    }

    @Override
    public void init(Config.Scope config) {
        this.webhookUrl = config.get("webhookUrl",
                "http://localhost:8081/internal/webhook/keycloak/user-registered");
        this.apiKey = config.get("apiKey", "");

        if (apiKey.isBlank()) {
            LOG.warning("No API key configured for " + PROVIDER_ID
                    + " — webhook calls will be unauthenticated!");
        }

        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();

        LOG.info( "{} initialized — webhookUrl= {}" ,PROVIDER_ID,webhookUrl);
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        // No post-initialization needed
    }

    @Override
    public void close() {
        // HttpClient doesn't require explicit close in Java 21
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }
}
