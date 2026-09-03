package com.project_aegis.keycloak.event;

import org.keycloak.events.Event;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RealmProvider;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserRegistrationEventListenerProvider Unit Tests")
class UserRegistrationEventListenerProviderTest {

    @Mock
    private KeycloakSession session;

    @Mock
    private HttpClient httpClient;

    @Mock
    private RealmProvider realmProvider;

    @Mock
    private UserProvider userProvider;

    @Mock
    private RealmModel realm;

    @Mock
    private UserModel user;

    @Mock
    private HttpResponse<String> httpResponse;

    private UserRegistrationEventListenerProvider provider;

    private static final String WEBHOOK_URL = "http://user-service:8081/internal/webhook/keycloak/user-registered";
    private static final String API_KEY = "test-api-key";

    @BeforeEach
    void setUp() {
        provider = new UserRegistrationEventListenerProvider(session, httpClient, WEBHOOK_URL, API_KEY);
    }

    // ──────────────────────────────────────────────────────────────
    //  onEvent (standard events)
    // ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("onEvent")
    class OnEvent {

        @Test
        @DisplayName("should send HTTP POST to webhook on REGISTER event")
        void shouldSendWebhookOnRegister() throws Exception {
            Event event = new Event();
            event.setType(EventType.REGISTER);
            event.setRealmId("test-realm");
            event.setUserId("user-123");

            when(session.realms()).thenReturn(realmProvider);
            when(realmProvider.getRealm("test-realm")).thenReturn(realm);
            when(session.users()).thenReturn(userProvider);
            when(userProvider.getUserById(realm, "user-123")).thenReturn(user);
            when(user.getId()).thenReturn("user-123");
            when(user.getEmail()).thenReturn("test@example.com");
            when(user.getFirstName()).thenReturn("John");
            when(user.getLastName()).thenReturn("Doe");
            when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                    .thenReturn(httpResponse);
            when(httpResponse.statusCode()).thenReturn(200);

            provider.onEvent(event);

            verify(httpClient).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
        }

        @Test
        @DisplayName("should ignore non-REGISTER events")
        void shouldIgnoreNonRegisterEvents() throws Exception {
            Event event = new Event();
            event.setType(EventType.LOGIN);

            provider.onEvent(event);

            verify(httpClient, never()).send(any(), any());
        }

        @Test
        @DisplayName("should log warning and return when user is not found")
        void shouldHandleUserNotFound() throws Exception {
            Event event = new Event();
            event.setType(EventType.REGISTER);
            event.setRealmId("test-realm");
            event.setUserId("missing-user");

            when(session.realms()).thenReturn(realmProvider);
            when(realmProvider.getRealm("test-realm")).thenReturn(realm);
            when(session.users()).thenReturn(userProvider);
            when(userProvider.getUserById(realm, "missing-user")).thenReturn(null);

            provider.onEvent(event);

            verify(httpClient, never()).send(any(), any());
        }

        @Test
        @DisplayName("should log error but not throw when HTTP call fails with IOException")
        void shouldHandleIOException() throws Exception {
            Event event = new Event();
            event.setType(EventType.REGISTER);
            event.setRealmId("test-realm");
            event.setUserId("user-123");

            when(session.realms()).thenReturn(realmProvider);
            when(realmProvider.getRealm("test-realm")).thenReturn(realm);
            when(session.users()).thenReturn(userProvider);
            when(userProvider.getUserById(realm, "user-123")).thenReturn(user);
            when(user.getId()).thenReturn("user-123");
            when(user.getEmail()).thenReturn("test@example.com");
            when(user.getFirstName()).thenReturn("John");
            when(user.getLastName()).thenReturn("Doe");
            when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                    .thenThrow(new IOException("Connection refused"));

            // Should not throw — errors are swallowed
            provider.onEvent(event);

            verify(httpClient).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
        }

        @Test
        @DisplayName("should log warning when webhook returns non-2xx status")
        void shouldLogWarningOnNon2xxStatus() throws Exception {
            Event event = new Event();
            event.setType(EventType.REGISTER);
            event.setRealmId("test-realm");
            event.setUserId("user-123");

            when(session.realms()).thenReturn(realmProvider);
            when(realmProvider.getRealm("test-realm")).thenReturn(realm);
            when(session.users()).thenReturn(userProvider);
            when(userProvider.getUserById(realm, "user-123")).thenReturn(user);
            when(user.getId()).thenReturn("user-123");
            when(user.getEmail()).thenReturn("test@example.com");
            when(user.getFirstName()).thenReturn("John");
            when(user.getLastName()).thenReturn("Doe");
            when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                    .thenReturn(httpResponse);
            when(httpResponse.statusCode()).thenReturn(500);
            when(httpResponse.body()).thenReturn("Internal Server Error");

            // Should not throw
            provider.onEvent(event);

            verify(httpClient).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
        }
    }

    // ──────────────────────────────────────────────────────────────
    //  onEvent (admin events) and close
    // ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("onEvent for AdminEvent should not throw")
    void adminEventShouldNotThrow() {
        AdminEvent adminEvent = new AdminEvent();
        provider.onEvent(adminEvent, true);
        // No exception means success — admin events are intentionally no-op
    }

    @Test
    @DisplayName("close should not throw")
    void closeShouldNotThrow() {
        provider.close();
        // No exception means success
    }
}
