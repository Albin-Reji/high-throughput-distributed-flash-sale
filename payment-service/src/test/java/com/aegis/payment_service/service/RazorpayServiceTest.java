package com.aegis.payment_service.service;

import com.aegis.payment_service.config.RazorpayProperties;
import com.aegis.payment_service.dto.request.CreatePaymentOrderRequest;
import com.aegis.payment_service.dto.response.PaymentOrderResponse;
import com.aegis.payment_service.entity.Payment;
import com.aegis.payment_service.entity.PaymentStatus;
import com.aegis.payment_service.exception.PaymentGatewayException;
import com.aegis.payment_service.repository.PaymentRepository;
import com.razorpay.Order;
import com.razorpay.OrderClient;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RazorpayService Unit Tests")
class RazorpayServiceTest {

    @Mock
    private RazorpayClient razorpayClient;

    @Mock
    private OrderClient orderClient;

    @Mock
    private RazorpayProperties razorpayProperties;

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private RazorpayService razorpayService;

    private UUID customerId;
    private UUID orderId;

    @BeforeEach
    void setUp() {
        customerId = UUID.randomUUID();
        orderId = UUID.randomUUID();
        razorpayClient.orders = orderClient;
    }

    @Test
    @DisplayName("Should create a Razorpay order and persist Payment entity successfully")
    void shouldCreatePaymentOrderSuccessfully() throws RazorpayException {
        // Arrange
        CreatePaymentOrderRequest request = CreatePaymentOrderRequest.builder()
                .orderId(orderId)
                .amount(new BigDecimal("500.00"))
                .currency("INR")
                .build();

        JSONObject razorpayResponse = new JSONObject();
        razorpayResponse.put("id", "order_test_12345");
        razorpayResponse.put("status", "created");
        razorpayResponse.put("amount", 50000);
        razorpayResponse.put("currency", "INR");
        Order mockOrder = new Order(razorpayResponse);

        when(orderClient.create(any(JSONObject.class))).thenReturn(mockOrder);
        when(razorpayProperties.getKeyId()).thenReturn("rzp_test_dummy");

        Payment savedPayment = Payment.builder()
                .id(UUID.randomUUID())
                .orderId(orderId)
                .customerId(customerId)
                .amount(new BigDecimal("500.00"))
                .currency("INR")
                .status(PaymentStatus.PENDING)
                .gatewayOrderId("order_test_12345")
                .build();
        when(paymentRepository.save(any(Payment.class))).thenReturn(savedPayment);

        // Act
        PaymentOrderResponse response = razorpayService.createPaymentOrder(customerId, request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getRazorpayOrderId()).isEqualTo("order_test_12345");
        assertThat(response.getAmount()).isEqualByComparingTo(new BigDecimal("500.00"));
        assertThat(response.getCurrency()).isEqualTo("INR");
        assertThat(response.getStatus()).isEqualTo("created");
        assertThat(response.getRazorpayKeyId()).isEqualTo("rzp_test_dummy");
        assertThat(response.getPaymentId()).isNotNull();

        // Verify Payment entity was saved with correct fields
        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(paymentCaptor.capture());
        Payment captured = paymentCaptor.getValue();
        assertThat(captured.getOrderId()).isEqualTo(orderId);
        assertThat(captured.getCustomerId()).isEqualTo(customerId);
        assertThat(captured.getAmount()).isEqualByComparingTo(new BigDecimal("500.00"));
        assertThat(captured.getCurrency()).isEqualTo("INR");
        assertThat(captured.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(captured.getGatewayOrderId()).isEqualTo("order_test_12345");
    }

    @Test
    @DisplayName("Should throw PaymentGatewayException when Razorpay API fails")
    void shouldThrowPaymentGatewayExceptionOnRazorpayFailure() throws RazorpayException {
        // Arrange
        CreatePaymentOrderRequest request = CreatePaymentOrderRequest.builder()
                .orderId(orderId)
                .amount(new BigDecimal("100.00"))
                .currency("INR")
                .build();

        when(orderClient.create(any(JSONObject.class)))
                .thenThrow(new RazorpayException("API error"));

        // Act & Assert
        assertThatThrownBy(() -> razorpayService.createPaymentOrder(customerId, request))
                .isInstanceOf(PaymentGatewayException.class)
                .hasMessageContaining("Failed to create payment order with gateway");

        verify(paymentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should convert amount to paise correctly (₹500.00 = 50000 paise)")
    void shouldConvertAmountToPaiseCorrectly() throws RazorpayException {
        // Arrange
        CreatePaymentOrderRequest request = CreatePaymentOrderRequest.builder()
                .orderId(orderId)
                .amount(new BigDecimal("10.50"))
                .currency("INR")
                .build();

        JSONObject razorpayResponse = new JSONObject();
        razorpayResponse.put("id", "order_test_67890");
        razorpayResponse.put("status", "created");
        razorpayResponse.put("amount", 1050);
        razorpayResponse.put("currency", "INR");
        Order mockOrder = new Order(razorpayResponse);

        when(orderClient.create(any(JSONObject.class))).thenReturn(mockOrder);
        when(razorpayProperties.getKeyId()).thenReturn("rzp_test_dummy");
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment p = invocation.getArgument(0);
            p.setId(UUID.randomUUID());
            return p;
        });

        // Act
        razorpayService.createPaymentOrder(customerId, request);

        // Assert - verify the amount sent to Razorpay is in paise
        ArgumentCaptor<JSONObject> optionsCaptor = ArgumentCaptor.forClass(JSONObject.class);
        verify(orderClient).create(optionsCaptor.capture());
        JSONObject capturedOptions = optionsCaptor.getValue();
        assertThat(capturedOptions.getInt("amount")).isEqualTo(1050);
        assertThat(capturedOptions.getString("currency")).isEqualTo("INR");
    }
}
