package com.aegis.payment_service.service;

import com.aegis.payment_service.config.RazorpayProperties;
import com.aegis.payment_service.dto.request.CreatePaymentOrderRequest;
import com.aegis.payment_service.dto.response.PaymentOrderResponse;
import com.aegis.payment_service.entity.Payment;
import com.aegis.payment_service.entity.PaymentStatus;
import com.aegis.payment_service.exception.PaymentGatewayException;
import com.aegis.payment_service.repository.PaymentRepository;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RazorpayService {

    private final RazorpayClient razorpayClient;
    private final RazorpayProperties razorpayProperties;
    private final PaymentRepository paymentRepository;

    /**
     * Creates a Razorpay order and persists the corresponding Payment entity.
     *
     * @param customerId the UUID of the authenticated customer
     * @param request    the checkout request containing orderId, amount, and currency
     * @return a response containing the Razorpay order ID and public key for the frontend
     */
    @Transactional
    public PaymentOrderResponse createPaymentOrder(UUID customerId, CreatePaymentOrderRequest request) {
        int amountInSmallestUnit = convertToSmallestUnit(request.getAmount(), request.getCurrency());
        String receipt = "receipt_" + request.getOrderId();

        JSONObject options = new JSONObject();
        options.put("amount", amountInSmallestUnit);
        options.put("currency", request.getCurrency());
        options.put("receipt", receipt);

        Order razorpayOrder;
        try {
            razorpayOrder = razorpayClient.orders.create(options);
        } catch (RazorpayException e) {
            log.error("Failed to create Razorpay order for orderId={}: {}", request.getOrderId(), e.getMessage());
            throw new PaymentGatewayException("Failed to create payment order with gateway", e);
        }

        String razorpayOrderId = razorpayOrder.get("id");
        String razorpayStatus = razorpayOrder.get("status");

        log.info("Razorpay order created: razorpayOrderId={}, orderId={}, amount={} {}",
                razorpayOrderId, request.getOrderId(), request.getAmount(), request.getCurrency());

        Payment payment = Payment.builder()
                .orderId(request.getOrderId())
                .customerId(customerId)
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .status(PaymentStatus.PENDING)
                .gatewayOrderId(razorpayOrderId)
                .build();

        payment = paymentRepository.save(payment);

        return PaymentOrderResponse.builder()
                .paymentId(payment.getId())
                .razorpayOrderId(razorpayOrderId)
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .status(razorpayStatus)
                .razorpayKeyId(razorpayProperties.getKeyId())
                .build();
    }

    /**
     * Converts a BigDecimal amount to the smallest currency unit (e.g. paise for INR, cents for USD).
     * Razorpay expects amounts in smallest units — ₹100.00 = 10000 paise.
     */
    private int convertToSmallestUnit(BigDecimal amount, String currency) {
        // All Razorpay-supported currencies use 2-decimal subunits (paise, cents, etc.)
        return amount.multiply(BigDecimal.valueOf(100))
                .intValueExact();
    }
}
