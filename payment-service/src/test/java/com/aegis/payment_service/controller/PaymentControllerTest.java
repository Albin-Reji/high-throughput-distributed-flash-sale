package com.aegis.payment_service.controller;

import com.aegis.payment_service.config.SecurityConfig;
import com.aegis.payment_service.dto.request.CreatePaymentOrderRequest;
import com.aegis.payment_service.dto.response.PaymentOrderResponse;
import com.aegis.payment_service.exception.GlobalExceptionHandler;
import com.aegis.payment_service.exception.PaymentGatewayException;
import com.aegis.payment_service.security.KeycloakJwtAuthenticationConverter;
import com.aegis.payment_service.security.SecurityAccessDeniedHandler;
import com.aegis.payment_service.security.SecurityAuthenticationEntryPoint;
import com.aegis.payment_service.service.RazorpayService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PaymentController.class)
@Import({
        GlobalExceptionHandler.class,
        SecurityConfig.class,
        KeycloakJwtAuthenticationConverter.class,
        SecurityAuthenticationEntryPoint.class,
        SecurityAccessDeniedHandler.class
})
@DisplayName("PaymentController Web Layer Tests")
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private RazorpayService razorpayService;

    @Test
    @DisplayName("POST /api/v1/payments/checkout - returns 201 Created and order details")
    void shouldCreatePaymentOrderSuccessfully() throws Exception {
        UUID customerId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();

        PaymentOrderResponse response = PaymentOrderResponse.builder()
                .paymentId(paymentId)
                .razorpayOrderId("order_test_999")
                .amount(new BigDecimal("999.00"))
                .currency("INR")
                .status("created")
                .razorpayKeyId("rzp_test_dummy_key")
                .build();

        when(razorpayService.createPaymentOrder(eq(customerId), any(CreatePaymentOrderRequest.class)))
                .thenReturn(response);

        String json = """
                {
                    "orderId": "%s",
                    "amount": 999.00,
                    "currency": "INR"
                }
                """.formatted(orderId);

        mockMvc.perform(post("/api/v1/payments/checkout")
                        .with(jwt().jwt(builder -> builder.subject(customerId.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.razorpayOrderId").value("order_test_999"))
                .andExpect(jsonPath("$.data.amount").value(999.00))
                .andExpect(jsonPath("$.data.currency").value("INR"))
                .andExpect(jsonPath("$.data.status").value("created"))
                .andExpect(jsonPath("$.data.razorpayKeyId").value("rzp_test_dummy_key"))
                .andExpect(jsonPath("$.data.paymentId").value(paymentId.toString()));
    }

    @Test
    @DisplayName("POST /api/v1/payments/checkout - returns 400 Bad Request when amount is negative")
    void shouldReturn400WhenAmountIsNegative() throws Exception {
        UUID customerId = UUID.randomUUID();
        String json = """
                {
                    "orderId": "%s",
                    "amount": -10.00,
                    "currency": "INR"
                }
                """.formatted(UUID.randomUUID());

        mockMvc.perform(post("/api/v1/payments/checkout")
                        .with(jwt().jwt(builder -> builder.subject(customerId.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("POST /api/v1/payments/checkout - returns 400 Bad Request when orderId is missing")
    void shouldReturn400WhenOrderIdIsMissing() throws Exception {
        UUID customerId = UUID.randomUUID();
        String json = """
                {
                    "amount": 100.00,
                    "currency": "INR"
                }
                """;

        mockMvc.perform(post("/api/v1/payments/checkout")
                        .with(jwt().jwt(builder -> builder.subject(customerId.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("POST /api/v1/payments/checkout - returns 502 Bad Gateway when Razorpay throws PaymentGatewayException")
    void shouldReturn502WhenGatewayFails() throws Exception {
        UUID customerId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();

        when(razorpayService.createPaymentOrder(eq(customerId), any(CreatePaymentOrderRequest.class)))
                .thenThrow(new PaymentGatewayException("Failed to create payment order with gateway: Network timeout"));

        String json = """
                {
                    "orderId": "%s",
                    "amount": 500.00,
                    "currency": "INR"
                }
                """.formatted(orderId);

        mockMvc.perform(post("/api/v1/payments/checkout")
                        .with(jwt().jwt(builder -> builder.subject(customerId.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Payment gateway error: Failed to create payment order with gateway: Network timeout"));
    }

    @Test
    @DisplayName("POST /api/v1/payments/checkout - returns 401 Unauthorized when unauthenticated")
    void shouldReturn401WhenUnauthenticated() throws Exception {
        String json = """
                {
                    "orderId": "%s",
                    "amount": 500.00,
                    "currency": "INR"
                }
                """.formatted(UUID.randomUUID());

        mockMvc.perform(post("/api/v1/payments/checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isUnauthorized());
    }
}
