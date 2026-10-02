package com.aegis.payment_service.service;

import com.aegis.payment_service.config.RazorpayProperties;
import com.aegis.payment_service.entity.Payment;
import com.aegis.payment_service.entity.PaymentStatus;
import com.aegis.payment_service.exception.InvalidWebhookSignatureException;
import com.aegis.payment_service.repository.PaymentRepository;
import com.razorpay.Utils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RazorpayWebhookService Unit Tests")
class RazorpayWebhookServiceTest {

    private static final String WEBHOOK_SECRET = "super_secret_webhook_key_12345";

    @Mock
    private RazorpayProperties razorpayProperties;

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private RazorpayWebhookService webhookService;

    @BeforeEach
    void setUp() {
        lenient().when(razorpayProperties.getWebhookSecret()).thenReturn(WEBHOOK_SECRET);
    }

    @Test
    @DisplayName("verifySignature - succeeds when signature matches payload HMAC")
    void shouldVerifySignatureSuccessfully() throws Exception {
        String payload = "{\"event\":\"payment.captured\"}";
        String signature = Utils.getHash(payload, WEBHOOK_SECRET);

        webhookService.verifySignature(payload, signature);
    }

    @Test
    @DisplayName("verifySignature - throws InvalidWebhookSignatureException when signature header is missing or blank")
    void shouldThrowWhenSignatureIsBlank() {
        assertThatThrownBy(() -> webhookService.verifySignature("{}", null))
                .isInstanceOf(InvalidWebhookSignatureException.class)
                .hasMessageContaining("Missing X-Razorpay-Signature");

        assertThatThrownBy(() -> webhookService.verifySignature("{}", "   "))
                .isInstanceOf(InvalidWebhookSignatureException.class)
                .hasMessageContaining("Missing X-Razorpay-Signature");
    }

    @Test
    @DisplayName("verifySignature - throws InvalidWebhookSignatureException when signature does not match")
    void shouldThrowWhenSignatureMismatch() {
        String payload = "{\"event\":\"payment.captured\"}";
        String invalidSignature = "invalid_signature_hex_12345";

        assertThatThrownBy(() -> webhookService.verifySignature(payload, invalidSignature))
                .isInstanceOf(InvalidWebhookSignatureException.class)
                .hasMessageContaining("Signature does not match payload");
    }

    @Test
    @DisplayName("verifySignature - throws InvalidWebhookSignatureException when secret is not configured")
    void shouldThrowWhenSecretNotConfigured() {
        when(razorpayProperties.getWebhookSecret()).thenReturn(null);

        assertThatThrownBy(() -> webhookService.verifySignature("{}", "dummy_sig"))
                .isInstanceOf(InvalidWebhookSignatureException.class)
                .hasMessageContaining("Server webhook secret is not configured");
    }

    @Test
    @DisplayName("processWebhook - updates payment to COMPLETED upon payment.captured event")
    void shouldProcessPaymentCapturedEvent() throws Exception {
        String orderId = "order_test_12345";
        String txnId = "pay_test_67890";
        String payload = createWebhookPayload("payment.captured", orderId, txnId);
        String signature = Utils.getHash(payload, WEBHOOK_SECRET);

        Payment payment = Payment.builder()
                .id(UUID.randomUUID())
                .orderId(UUID.randomUUID())
                .customerId(UUID.randomUUID())
                .amount(new BigDecimal("500.00"))
                .currency("INR")
                .status(PaymentStatus.PENDING)
                .gatewayOrderId(orderId)
                .build();

        when(paymentRepository.findByGatewayOrderId(orderId)).thenReturn(Optional.of(payment));

        webhookService.processWebhook(payload, signature);

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(captor.capture());

        Payment updated = captor.getValue();
        assertThat(updated.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(updated.getGatewayTxnId()).isEqualTo(txnId);
        assertThat(updated.getGatewaySignature()).isEqualTo(signature);
    }

    @Test
    @DisplayName("processWebhook - updates payment to FAILED upon payment.failed event")
    void shouldProcessPaymentFailedEvent() throws Exception {
        String orderId = "order_fail_99999";
        String txnId = "pay_fail_88888";
        String payload = createWebhookPayload("payment.failed", orderId, txnId);
        String signature = Utils.getHash(payload, WEBHOOK_SECRET);

        Payment payment = Payment.builder()
                .id(UUID.randomUUID())
                .orderId(UUID.randomUUID())
                .customerId(UUID.randomUUID())
                .amount(new BigDecimal("999.00"))
                .currency("INR")
                .status(PaymentStatus.PENDING)
                .gatewayOrderId(orderId)
                .build();

        when(paymentRepository.findByGatewayOrderId(orderId)).thenReturn(Optional.of(payment));

        webhookService.processWebhook(payload, signature);

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(captor.capture());

        Payment updated = captor.getValue();
        assertThat(updated.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(updated.getGatewayTxnId()).isEqualTo(txnId);
        assertThat(updated.getGatewaySignature()).isEqualTo(signature);
    }

    @Test
    @DisplayName("processWebhook - idempotent when payment is already COMPLETED")
    void shouldBeIdempotentWhenPaymentAlreadyCompleted() throws Exception {
        String orderId = "order_already_paid";
        String txnId = "pay_already_captured";
        String payload = createWebhookPayload("payment.captured", orderId, txnId);
        String signature = Utils.getHash(payload, WEBHOOK_SECRET);

        Payment payment = Payment.builder()
                .id(UUID.randomUUID())
                .orderId(UUID.randomUUID())
                .customerId(UUID.randomUUID())
                .amount(new BigDecimal("100.00"))
                .currency("INR")
                .status(PaymentStatus.COMPLETED)
                .gatewayOrderId(orderId)
                .gatewayTxnId(txnId)
                .build();

        when(paymentRepository.findByGatewayOrderId(orderId)).thenReturn(Optional.of(payment));

        webhookService.processWebhook(payload, signature);

        // Verify save was not called again
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    @DisplayName("processWebhook - handles missing order gracefully without error")
    void shouldHandleMissingPaymentGracefully() throws Exception {
        String orderId = "order_non_existent";
        String txnId = "pay_unknown";
        String payload = createWebhookPayload("payment.captured", orderId, txnId);
        String signature = Utils.getHash(payload, WEBHOOK_SECRET);

        when(paymentRepository.findByGatewayOrderId(orderId)).thenReturn(Optional.empty());

        webhookService.processWebhook(payload, signature);

        verify(paymentRepository, never()).save(any());
    }

    @Test
    @DisplayName("processWebhook - throws InvalidWebhookSignatureException on malformed JSON")
    void shouldThrowOnMalformedJson() {
        String invalidJson = "{ malformed_json_without_closing ";
        String signature = "any_signature";

        assertThatThrownBy(() -> webhookService.processWebhook(invalidJson, signature))
                .isInstanceOf(InvalidWebhookSignatureException.class);
    }

    private String createWebhookPayload(String event, String orderId, String txnId) {
        return """
                {
                    "entity": "event",
                    "event": "%s",
                    "payload": {
                        "payment": {
                            "entity": {
                                "id": "%s",
                                "order_id": "%s",
                                "amount": 50000,
                                "currency": "INR",
                                "status": "captured"
                            }
                        }
                    }
                }
                """.formatted(event, txnId, orderId);
    }
}
