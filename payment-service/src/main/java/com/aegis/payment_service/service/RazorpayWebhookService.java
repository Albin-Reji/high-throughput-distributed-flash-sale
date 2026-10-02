package com.aegis.payment_service.service;

import com.aegis.payment_service.config.RazorpayProperties;
import com.aegis.payment_service.entity.Payment;
import com.aegis.payment_service.entity.PaymentStatus;
import com.aegis.payment_service.exception.InvalidWebhookSignatureException;
import com.aegis.payment_service.repository.PaymentRepository;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RazorpayWebhookService {

    private final RazorpayProperties razorpayProperties;
    private final PaymentRepository paymentRepository;

    /**
     * Verifies the cryptographic HMAC-SHA256 signature sent by Razorpay in the X-Razorpay-Signature header.
     *
     * @param payload   raw request body payload as string
     * @param signature the signature header value
     * @throws InvalidWebhookSignatureException if signature is missing or verification fails
     */
    public void verifySignature(String payload, String signature) {
        if (signature == null || signature.isBlank()) {
            throw new InvalidWebhookSignatureException("Missing X-Razorpay-Signature header");
        }

        String webhookSecret = razorpayProperties.getWebhookSecret();
        if (webhookSecret == null || webhookSecret.isBlank()) {
            log.error("Razorpay webhook secret is not configured in application properties");
            throw new InvalidWebhookSignatureException("Server webhook secret is not configured");
        }

        log.info("Verifying webhook signature. Secret length: {}, signature: [{}]",
                webhookSecret.length(), signature);
        log.debug("Verifying against payload: [{}]", payload);

        try {
            boolean isValid = Utils.verifyWebhookSignature(payload, signature, webhookSecret);
            if (!isValid) {
                log.warn("Webhook signature mismatch for incoming Razorpay payload");
                throw new InvalidWebhookSignatureException("Signature does not match payload");
            }
        } catch (RazorpayException e) {
            log.error("Failed to verify Razorpay webhook signature: {}", e.getMessage());
            throw new InvalidWebhookSignatureException("Signature verification error: " + e.getMessage(), e);
        }
    }

    /**
     * Validates the webhook signature and processes payment status updates.
     *
     * @param rawPayload raw string JSON payload from Razorpay
     * @param signature  X-Razorpay-Signature header
     */
    @Transactional
    public void processWebhook(String rawPayload, String signature) {
        verifySignature(rawPayload, signature);

        JSONObject eventJson;
        try {
            eventJson = new JSONObject(rawPayload);
        } catch (Exception e) {
            log.error("Failed to parse webhook JSON payload: {}", e.getMessage());
            throw new InvalidWebhookSignatureException("Malformed JSON payload: " + e.getMessage());
        }

        String event = eventJson.optString("event");
        log.info("Processing verified Razorpay webhook event: {}", event);

        JSONObject payloadObj = eventJson.optJSONObject("payload");
        if (payloadObj == null) {
            log.warn("Webhook JSON missing 'payload' object");
            return;
        }

        JSONObject paymentObj = payloadObj.optJSONObject("payment");
        if (paymentObj == null) {
            log.debug("Webhook event '{}' does not contain 'payment' payload, skipping status transition", event);
            return;
        }

        JSONObject entity = paymentObj.optJSONObject("entity");
        if (entity == null) {
            log.warn("Payment payload missing 'entity' object");
            return;
        }

        String gatewayTxnId = entity.optString("id");
        String gatewayOrderId = entity.optString("order_id");

        if (gatewayOrderId == null || gatewayOrderId.isBlank()) {
            log.warn("Payment entity does not specify 'order_id'");
            return;
        }

        handlePaymentEvent(event, gatewayOrderId, gatewayTxnId, signature);
    }

    private void handlePaymentEvent(String event, String gatewayOrderId, String gatewayTxnId, String signature) {
        Optional<Payment> optionalPayment = paymentRepository.findByGatewayOrderId(gatewayOrderId);
        if (optionalPayment.isEmpty()) {
            log.warn("No payment entity found matching gatewayOrderId={}", gatewayOrderId);
            return;
        }

        Payment payment = optionalPayment.get();

        switch (event) {
            case "payment.captured", "order.paid" -> {
                if (payment.getStatus() == PaymentStatus.COMPLETED) {
                    log.info("Payment already COMPLETED for gatewayOrderId={}, ignoring duplicate webhook", gatewayOrderId);
                    return;
                }
                payment.setStatus(PaymentStatus.COMPLETED);
                payment.setGatewayTxnId(gatewayTxnId);
                payment.setGatewaySignature(signature);
                paymentRepository.save(payment);
                log.info("Payment successfully marked COMPLETED: paymentId={}, orderId={}, gatewayTxnId={}",
                        payment.getId(), payment.getOrderId(), gatewayTxnId);
            }
            case "payment.failed" -> {
                if (payment.getStatus() == PaymentStatus.COMPLETED) {
                    log.warn("Received payment.failed for already COMPLETED paymentId={}, ignoring", payment.getId());
                    return;
                }
                payment.setStatus(PaymentStatus.FAILED);
                payment.setGatewayTxnId(gatewayTxnId);
                payment.setGatewaySignature(signature);
                paymentRepository.save(payment);
                log.info("Payment marked FAILED: paymentId={}, orderId={}, gatewayTxnId={}",
                        payment.getId(), payment.getOrderId(), gatewayTxnId);
            }
            default -> log.debug("Unhandled Razorpay webhook event '{}' for gatewayOrderId={}", event, gatewayOrderId);
        }
    }
}
