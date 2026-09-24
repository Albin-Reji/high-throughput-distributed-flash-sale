package com.project_aegis.order_service.client;

import com.project_aegis.order_service.client.dto.CustomerAddressClientResponse;
import com.project_aegis.order_service.client.dto.SkuClientResponse;
import com.project_aegis.order_service.client.dto.StockReservationClientRequest;
import com.project_aegis.order_service.config.InternalApiProperties;
import com.project_aegis.order_service.dto.response.ApiResponse;
import com.project_aegis.order_service.exception.GlobalExceptionHandler;
import com.project_aegis.order_service.exception.ResourceNotFoundException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Collections;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Resilience4j Circuit Breaker & Time Limiter Tests")
class ClientResilienceTest {

    private CircuitBreakerConfig defaultCircuitBreakerConfig;
    private CircuitBreakerRegistry circuitBreakerRegistry;

    private TimeLimiterConfig defaultTimeLimiterConfig;
    private TimeLimiterRegistry timeLimiterRegistry;

    @Mock
    private RestClient productRestClient;

    @Mock
    private RestClient userRestClient;

    @Mock
    private RestClient inventoryRestClient;

    @Mock
    private RestClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    private RestClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private RestClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private RestClient.RequestBodySpec requestBodySpec;

    @Mock
    private RestClient.ResponseSpec responseSpec;

    private InternalApiProperties internalApiProperties;

    @BeforeEach
    void setUp() {
        defaultCircuitBreakerConfig = CircuitBreakerConfig.custom()
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(20)
                .minimumNumberOfCalls(20)
                .failureRateThreshold(50.0f)
                .slowCallRateThreshold(50.0f)
                .slowCallDurationThreshold(Duration.ofSeconds(3))
                .waitDurationInOpenState(Duration.ofSeconds(10))
                .permittedNumberOfCallsInHalfOpenState(5)
                .automaticTransitionFromOpenToHalfOpenEnabled(true)
                .ignoreExceptions(ResourceNotFoundException.class)
                .build();

        circuitBreakerRegistry = CircuitBreakerRegistry.of(
                Collections.singletonMap("default", defaultCircuitBreakerConfig)
        );

        defaultTimeLimiterConfig = TimeLimiterConfig.custom()
                .timeoutDuration(Duration.ofSeconds(3))
                .cancelRunningFuture(true)
                .build();

        timeLimiterRegistry = TimeLimiterRegistry.of(
                Collections.singletonMap("default", defaultTimeLimiterConfig)
        );

        internalApiProperties = new InternalApiProperties();
        internalApiProperties.setOrderKey("test-order-key");
        internalApiProperties.setInventoryKey("test-inventory-key");
    }

    @Nested
    @DisplayName("ProductServiceClient Circuit Breaker")
    class ProductServiceClientCircuitBreakerTests {

        @Test
        @DisplayName("should open circuit and fast-fail after 50% failures over 20 calls")
        void shouldOpenCircuitAndFastFailAfter50PercentFailures() {
            CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("productService", defaultCircuitBreakerConfig);
            ProductServiceClient client = new ProductServiceClient(productRestClient);

            assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.CLOSED);

            UUID skuId = UUID.randomUUID();
            SkuClientResponse mockResponse = SkuClientResponse.builder()
                    .id(skuId)
                    .skuCode("SKU-TEST")
                    .productName("Test Product")
                    .price(BigDecimal.valueOf(100))
                    .build();

            // Mock successful RestClient chain
            when(productRestClient.get()).thenReturn(requestHeadersUriSpec);
            when(requestHeadersUriSpec.uri(anyString(), any(Object.class))).thenReturn(requestHeadersSpec);
            when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
            when(responseSpec.body(SkuClientResponse.class)).thenReturn(mockResponse);

            // Execute 10 successful calls wrapped with circuit breaker
            for (int i = 0; i < 10; i++) {
                SkuClientResponse res = cb.executeSupplier(() -> client.getSku(skuId, null));
                assertThat(res).isNotNull();
            }

            assertThat(cb.getMetrics().getNumberOfSuccessfulCalls()).isEqualTo(10);
            assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.CLOSED);

            // Mock failure RestClient chain (500 Internal Server Error)
            when(responseSpec.body(SkuClientResponse.class))
                    .thenThrow(new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR, "Product service down"));

            // Execute 10 failing calls wrapped with circuit breaker
            for (int i = 0; i < 10; i++) {
                assertThatThrownBy(() -> cb.executeSupplier(() -> client.getSku(skuId, null)))
                        .isInstanceOf(RestClientException.class);
            }

            // Total calls = 20 (10 success, 10 failed => 50% failure rate)
            // Circuit must now be OPEN
            assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.OPEN);
            assertThat(cb.getMetrics().getFailureRate()).isEqualTo(50.0f);

            // The 21st call must fast-fail with CallNotPermittedException without calling RestClient
            assertThatThrownBy(() -> cb.executeSupplier(() -> client.getSku(skuId, null)))
                    .isInstanceOf(CallNotPermittedException.class)
                    .hasMessageContaining("productService");
        }

        @Test
        @DisplayName("should not trip circuit when ResourceNotFoundException (404) is thrown")
        void shouldIgnoreResourceNotFoundException() {
            CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("productService", defaultCircuitBreakerConfig);
            ProductServiceClient client = new ProductServiceClient(productRestClient);

            UUID skuId = UUID.randomUUID();
            when(productRestClient.get()).thenReturn(requestHeadersUriSpec);
            when(requestHeadersUriSpec.uri(anyString(), any(Object.class))).thenReturn(requestHeadersSpec);
            when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
            when(responseSpec.body(SkuClientResponse.class)).thenReturn(null); // triggers ResourceNotFoundException

            for (int i = 0; i < 20; i++) {
                assertThatThrownBy(() -> cb.executeSupplier(() -> client.getSku(skuId, null)))
                        .isInstanceOf(ResourceNotFoundException.class);
            }

            // Circuit must remain CLOSED because ResourceNotFoundException is ignored
            assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
            assertThat(cb.getMetrics().getNumberOfFailedCalls()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("UserServiceClient Circuit Breaker")
    class UserServiceClientCircuitBreakerTests {

        @Test
        @DisplayName("should open circuit and fast-fail after 50% failures over 20 calls")
        void shouldOpenCircuitAndFastFailAfter50PercentFailures() {
            CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("userService", defaultCircuitBreakerConfig);
            UserServiceClient client = new UserServiceClient(userRestClient);

            assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.CLOSED);

            UUID addressId = UUID.randomUUID();
            CustomerAddressClientResponse mockAddress = CustomerAddressClientResponse.builder()
                    .id(addressId)
                    .recipientName("Test Customer")
                    .addressLine1("123 Test St")
                    .city("Bengaluru")
                    .state("Karnataka")
                    .postalCode("560001")
                    .country("India")
                    .build();

            ApiResponse<CustomerAddressClientResponse> apiResponse = ApiResponse.<CustomerAddressClientResponse>builder()
                    .success(true)
                    .data(mockAddress)
                    .build();

            // Mock successful RestClient chain
            when(userRestClient.get()).thenReturn(requestHeadersUriSpec);
            when(requestHeadersUriSpec.uri(anyString(), any(Object.class))).thenReturn(requestHeadersSpec);
            when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
            when(responseSpec.body(any(org.springframework.core.ParameterizedTypeReference.class))).thenReturn(apiResponse);

            // 10 successful calls
            for (int i = 0; i < 10; i++) {
                CustomerAddressClientResponse res = cb.executeSupplier(() -> client.getAddress(addressId, null));
                assertThat(res).isNotNull();
            }

            // Mock network/connection failure
            when(responseSpec.body(any(org.springframework.core.ParameterizedTypeReference.class)))
                    .thenThrow(new ResourceAccessException("Connection timed out to user-service"));

            // 10 failing calls
            for (int i = 0; i < 10; i++) {
                assertThatThrownBy(() -> cb.executeSupplier(() -> client.getAddress(addressId, null)))
                        .isInstanceOf(RestClientException.class);
            }

            // 50% failures over 20 calls -> OPEN
            assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.OPEN);

            // Call 21 fast-fails with CallNotPermittedException
            assertThatThrownBy(() -> cb.executeSupplier(() -> client.getAddress(addressId, null)))
                    .isInstanceOf(CallNotPermittedException.class)
                    .hasMessageContaining("userService");
        }
    }

    @Nested
    @DisplayName("InventoryServiceClient Circuit Breaker")
    class InventoryServiceClientCircuitBreakerTests {

        @Test
        @DisplayName("should open circuit and fast-fail after 50% failures over 20 calls")
        void shouldOpenCircuitAndFastFailAfter50PercentFailures() {
            CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("inventoryService", defaultCircuitBreakerConfig);
            InventoryServiceClient client = new InventoryServiceClient(internalApiProperties, inventoryRestClient);

            assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.CLOSED);

            StockReservationClientRequest request = StockReservationClientRequest.builder()
                    .orderId(UUID.randomUUID())
                    .customerId(UUID.randomUUID())
                    .items(Collections.emptyList())
                    .build();

            // Mock successful RestClient chain
            when(inventoryRestClient.post()).thenReturn(requestBodyUriSpec);
            when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
            when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestBodySpec);
            when(requestBodySpec.contentType(any(MediaType.class))).thenReturn(requestBodySpec);
            when(requestBodySpec.body(any(Object.class))).thenReturn(requestBodySpec);
            when(requestBodySpec.retrieve()).thenReturn(responseSpec);
            when(responseSpec.toBodilessEntity()).thenReturn(ResponseEntity.ok().build());

            // 10 successful calls
            for (int i = 0; i < 10; i++) {
                cb.executeRunnable(() -> client.reserveStock(request));
            }

            assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.CLOSED);

            // Mock 500 error from inventory-service
            when(responseSpec.toBodilessEntity())
                    .thenThrow(new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR, "Inventory DB unreachable"));

            // 10 failing calls
            for (int i = 0; i < 10; i++) {
                assertThatThrownBy(() -> cb.executeRunnable(() -> client.reserveStock(request)))
                        .isInstanceOf(RestClientException.class);
            }

            // 50% failures over 20 calls -> OPEN
            assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.OPEN);

            // Call 21 fast-fails with CallNotPermittedException
            assertThatThrownBy(() -> cb.executeRunnable(() -> client.reserveStock(request)))
                    .isInstanceOf(CallNotPermittedException.class)
                    .hasMessageContaining("inventoryService");
        }
    }

    @Nested
    @DisplayName("GlobalExceptionHandler CallNotPermittedException Handling")
    class GlobalExceptionHandlerTests {

        @Test
        @DisplayName("should return 503 SERVICE_UNAVAILABLE when circuit is open")
        void shouldReturn503OnCallNotPermittedException() {
            GlobalExceptionHandler handler = new GlobalExceptionHandler();

            CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("productService", defaultCircuitBreakerConfig);
            cb.transitionToOpenState();

            CallNotPermittedException exception = CallNotPermittedException.createCallNotPermittedException(cb);

            ResponseEntity<ApiResponse<Void>> response = handler.handleCallNotPermitted(exception);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getSuccess()).isFalse();
            assertThat(response.getBody().getMessage()).contains("productService");
            assertThat(response.getBody().getMessage()).contains("Circuit breaker is OPEN");
        }
    }

    @Nested
    @DisplayName("TimeLimiter Configuration")
    class TimeLimiterConfigurationTests {

        @Test
        @DisplayName("should configure 3-second timeout duration")
        void shouldVerifyTimeLimiterConfig() {
            TimeLimiter tl = timeLimiterRegistry.timeLimiter("productService", defaultTimeLimiterConfig);

            assertThat(tl.getTimeLimiterConfig().getTimeoutDuration()).isEqualTo(Duration.ofSeconds(3));
            assertThat(tl.getTimeLimiterConfig().shouldCancelRunningFuture()).isTrue();
        }
    }
}
