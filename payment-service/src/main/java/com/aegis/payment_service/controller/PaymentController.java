package com.aegis.payment_service.controller;

import com.aegis.payment_service.dto.request.CreatePaymentOrderRequest;
import com.aegis.payment_service.dto.response.ApiResponse;
import com.aegis.payment_service.dto.response.PaymentOrderResponse;
import com.aegis.payment_service.service.RazorpayService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final RazorpayService razorpayService;

    @PostMapping("/checkout")
    public ResponseEntity<ApiResponse<PaymentOrderResponse>> createPaymentOrder(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreatePaymentOrderRequest request
    ) {
        UUID customerId = UUID.fromString(Objects.requireNonNull(jwt.getSubject()));
        log.info("Payment checkout request: orderId={}, customerId={}", request.getOrderId(), customerId);

        PaymentOrderResponse response = razorpayService.createPaymentOrder(customerId, request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<PaymentOrderResponse>builder()
                        .success(true)
                        .message("Payment order created successfully")
                        .data(response)
                        .build());
    }
}
