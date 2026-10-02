package com.aegis.payment_service.service;

import com.aegis.payment_service.config.RazorpayProperties;
import com.aegis.payment_service.dto.request.CreatePaymentOrderRequest;
import com.aegis.payment_service.dto.response.PaymentOrderResponse;
import com.aegis.payment_service.repository.PaymentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test that creates a real Razorpay order using the Test Mode API.
 * Only runs when RAZORPAY_KEY_ID is set in the environment, preventing
 * CI/CD failures when credentials are not available.
 */
@SpringBootTest
@ActiveProfiles("dev")
@Transactional
@DisplayName("Razorpay Live API Integration Test")
@EnabledIfEnvironmentVariable(named = "RAZORPAY_KEY_ID", matches = "rzp_test_.+")
class RazorpayIntegrationTest {

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Autowired
    private RazorpayService razorpayService;

    @Autowired
    private RazorpayProperties razorpayProperties;

    @Autowired
    private PaymentRepository paymentRepository;

    @Test
    @DisplayName("RazorpayClient bean should be initialized with valid credentials")
    void razorpayClientBeanShouldBeInitialized() {
        assertThat(razorpayProperties.getKeyId()).isNotBlank();
        assertThat(razorpayProperties.getKeyId()).startsWith("rzp_test_");
        assertThat(razorpayProperties.getKeySecret()).isNotBlank();
    }

    @Test
    @DisplayName("Should create a Razorpay order via Test Mode API and receive a valid order ID")
    void shouldCreateRazorpayOrderViaTestApi() {
        UUID customerId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();

        CreatePaymentOrderRequest request = CreatePaymentOrderRequest.builder()
                .orderId(orderId)
                .amount(new BigDecimal("100.00"))
                .currency("INR")
                .build();

        PaymentOrderResponse response = razorpayService.createPaymentOrder(customerId, request);

        // Verify Razorpay returned a valid order
        assertThat(response).isNotNull();
        assertThat(response.getRazorpayOrderId()).isNotBlank();
        assertThat(response.getRazorpayOrderId()).startsWith("order_");
        assertThat(response.getAmount()).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(response.getCurrency()).isEqualTo("INR");
        assertThat(response.getStatus()).isEqualTo("created");
        assertThat(response.getPaymentId()).isNotNull();
        assertThat(response.getRazorpayKeyId()).startsWith("rzp_test_");
    }
}
