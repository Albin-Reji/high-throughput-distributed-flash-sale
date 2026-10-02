package com.aegis.payment_service.controller;

import com.aegis.payment_service.dto.response.ApiResponse;
import com.aegis.payment_service.service.RazorpayWebhookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/payments/webhooks")
@RequiredArgsConstructor
public class RazorpayWebhookController {

    private final RazorpayWebhookService razorpayWebhookService;

    @PostMapping("/razorpay")
    public ResponseEntity<ApiResponse<Void>> handleRazorpayWebhook(
            @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature,
            @RequestBody String rawPayload
    ) {
        log.info("Received Razorpay webhook request");
        razorpayWebhookService.processWebhook(rawPayload, signature);

        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Webhook processed successfully")
                .build());
    }
}
