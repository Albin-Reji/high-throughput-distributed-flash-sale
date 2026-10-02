package com.aegis.payment_service.config;

import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration(proxyBeanMethods = false)
@RequiredArgsConstructor
public class RazorpayConfig {

    private final RazorpayProperties razorpayProperties;

    @Bean
    public RazorpayClient razorpayClient() throws RazorpayException {
        String keyId = razorpayProperties.getKeyId();
        String keySecret = razorpayProperties.getKeySecret();

        if (keyId == null || keyId.isBlank() || keySecret == null || keySecret.isBlank()) {
            throw new IllegalStateException(
                    "Razorpay credentials are not configured. " +
                    "Set RAZORPAY_KEY_ID and RAZORPAY_KEY_SECRET environment variables."
            );
        }

        log.info("Initializing RazorpayClient with key ID: {}****",
                keyId.substring(0, Math.min(keyId.length(), 8)));

        return new RazorpayClient(keyId, keySecret);
    }
}
