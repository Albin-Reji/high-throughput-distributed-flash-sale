package com.project_aegis.keycloak.event;

import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Listens for {@link EventType#REGISTER} events in Keycloak and sends
 * a webhook to the user-service so that a {@code CustomerProfile} is
 * created in the application database.
 *
 * <p><b>Production notes:</b></p>
 * <ul>
 *   <li>This runs inside the Keycloak request thread. Keep it fast.</li>
 *   <li>Errors are logged but never re-thrown — registration must not fail
 *       because the downstream service is temporarily unavailable.</li>
 *   <li>The webhook is idempotent (user-service de-duplicates by keycloakUserId),
 *       so retries are safe.</li>
 * </ul>
 */
public class UserRegistrationEventListenerProvider implements EventListenerProvider {

    private static final Logger LOG =
            Logger.getLogger(UserRegistrationEventListenerProvider.class.getName());

    private final KeycloakSession session;
    private final HttpClient httpClient;
    private final String webhookUrl;
    private final String apiKey;

    public UserRegistrationEventListenerProvider(KeycloakSession session,
                                                  HttpClient httpClient,
                                                  String webhookUrl,
                                                  String apiKey) {
        this.session = session;
        this.httpClient = httpClient;
        this.webhookUrl = webhookUrl;
        this.apiKey = apiKey;
    }

    @Override
    public void onEvent(Event event) {
        if (!EventType.REGISTER.equals(event.getType())) {
            return;
        }

        try {
            RealmModel realm = session.realms().getRealm(event.getRealmId());
            UserModel user = session.users().getUserById(realm, event.getUserId());

            if (user == null) {
                LOG.warning("REGISTER event received but user not found: " + event.getUserId());
                return;
            }

            String jsonPayload = buildJsonPayload(user);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(webhookUrl))
                    .timeout(Duration.ofSeconds(5))
                    .header("Content-Type", "application/json")
                    .header("X-Internal-Api-Key", apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                LOG.info("User profile sync successful for keycloakUserId=" + user.getId()
                        + " (HTTP " + response.statusCode() + ")");
            } else {
                LOG.warning("User profile sync returned HTTP " + response.statusCode()
                        + " for keycloakUserId=" + user.getId()
                        + " — body: " + response.body());
            }

        } catch (RuntimeException | IOException | InterruptedException e) {
            // Log and swallow — registration must never fail due to webhook issues
            LOG.log(Level.SEVERE,
                    "Failed to sync user profile for event userId=" + event.getUserId(), e);
        }
    }

    @Override
    public void onEvent(AdminEvent event, boolean includeRepresentation) {
        // Admin events (e.g., admin creating a user) can be handled here if needed.
        // For now, we only handle self-registration via the standard event.
    }

    @Override
    public void close() {
        // Nothing to clean up — HttpClient is managed by the factory
    }

    /**
     * Builds a minimal JSON payload using string concatenation to avoid
     * bringing in a JSON library dependency into the Keycloak SPI JAR.
     */
    private String buildJsonPayload(UserModel user) {
        return "{" +
                "\"keycloakUserId\":\"" + escapeJson(user.getId()) + "\"," +
                "\"email\":\"" + escapeJson(user.getEmail()) + "\"," +
                "\"firstName\":\"" + escapeJson(user.getFirstName()) + "\"," +
                "\"lastName\":\"" + escapeJson(user.getLastName()) + "\"" +
                "}";
    }

    /**
     * Basic JSON string escaping for safety.
     */
    private String escapeJson(String value) {
        if (value == null) return "";
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
