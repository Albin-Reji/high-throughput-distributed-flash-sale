package com.aegis.payment_service;

import com.aegis.payment_service.entity.Payment;
import com.aegis.payment_service.entity.PaymentStatus;
import com.aegis.payment_service.repository.PaymentRepository;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("dev")
@DisplayName("Flyway Migration & PostgreSQL Schema Integration Test")
class PaymentFlywayPostgresIntegrationTest {

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Autowired
    private Flyway flyway;

    @Autowired
    private PaymentRepository paymentRepository;

    @Test
    @DisplayName("V1 Flyway migration should be applied successfully to payment_service_db")
    void flywayMigrationAppliedSuccessfully() {
        MigrationInfo current = flyway.info().current();
        assertThat(current).isNotNull();
        assertThat(current.getVersion().getVersion()).isEqualTo("1");
        assertThat(current.getDescription()).isEqualTo("init payment schema");
        assertThat(current.getState().isApplied()).isTrue();
    }

    @Test
    @DisplayName("Should successfully insert and query Payment from PostgreSQL")
    void shouldPersistAndRetrieveInPostgreSQL() {
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();

        Payment payment = Payment.builder()
                .orderId(orderId)
                .customerId(customerId)
                .amount(new BigDecimal("2499.50"))
                .currency("INR")
                .status(PaymentStatus.PENDING)
                .gatewayOrderId("order_pg_test_001")
                .gatewayTxnId("pay_pg_test_001")
                .gatewaySignature("sig_pg_test_001")
                .build();

        Payment saved = paymentRepository.save(payment);
        assertThat(saved.getId()).isNotNull();

        Optional<Payment> retrieved = paymentRepository.findById(saved.getId());
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getAmount()).isEqualByComparingTo(new BigDecimal("2499.50"));
        assertThat(retrieved.get().getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(retrieved.get().getCreatedAt()).isNotNull();
        assertThat(retrieved.get().getUpdatedAt()).isNotNull();

        // Cleanup
        paymentRepository.deleteById(saved.getId());
    }
}
