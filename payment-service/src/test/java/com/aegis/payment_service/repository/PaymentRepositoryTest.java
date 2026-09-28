package com.aegis.payment_service.repository;

import com.aegis.payment_service.entity.Payment;
import com.aegis.payment_service.entity.PaymentStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@DisplayName("PaymentRepository and Payment Entity Tests")
class PaymentRepositoryTest {

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Autowired
    private PaymentRepository paymentRepository;

    @Test
    @DisplayName("Should persist and retrieve a payment entity")
    void shouldPersistAndRetrievePayment() {
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();

        Payment payment = Payment.builder()
                .orderId(orderId)
                .customerId(customerId)
                .amount(new BigDecimal("1499.00"))
                .currency("INR")
                .status(PaymentStatus.PENDING)
                .gatewayOrderId("order_test_123456")
                .gatewayTxnId("pay_test_789012")
                .gatewaySignature("sig_test_abcdef")
                .build();

        Payment saved = paymentRepository.saveAndFlush(payment);

        Optional<Payment> found = paymentRepository.findById(saved.getId());
        assertThat(found).isPresent();
        Payment retrieved = found.get();

        assertThat(retrieved.getId()).isNotNull();
        assertThat(retrieved.getOrderId()).isEqualTo(orderId);
        assertThat(retrieved.getCustomerId()).isEqualTo(customerId);
        assertThat(retrieved.getAmount()).isEqualByComparingTo(new BigDecimal("1499.00"));
        assertThat(retrieved.getCurrency()).isEqualTo("INR");
        assertThat(retrieved.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(retrieved.getGatewayOrderId()).isEqualTo("order_test_123456");
        assertThat(retrieved.getGatewayTxnId()).isEqualTo("pay_test_789012");
        assertThat(retrieved.getGatewaySignature()).isEqualTo("sig_test_abcdef");
        assertThat(retrieved.getCreatedAt()).isNotNull();
        assertThat(retrieved.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should find payments by orderId")
    void shouldFindByOrderId() {
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();

        Payment payment1 = Payment.builder()
                .orderId(orderId)
                .customerId(customerId)
                .amount(new BigDecimal("500.00"))
                .currency("INR")
                .status(PaymentStatus.FAILED)
                .build();

        Payment payment2 = Payment.builder()
                .orderId(orderId)
                .customerId(customerId)
                .amount(new BigDecimal("500.00"))
                .currency("INR")
                .status(PaymentStatus.COMPLETED)
                .build();

        paymentRepository.save(payment1);
        paymentRepository.save(payment2);

        List<Payment> payments = paymentRepository.findByOrderId(orderId);
        assertThat(payments).hasSize(2);

        Optional<Payment> latest = paymentRepository.findFirstByOrderIdOrderByCreatedAtDesc(orderId);
        assertThat(latest).isPresent();
        assertThat(latest.get().getStatus()).isEqualTo(PaymentStatus.COMPLETED);
    }

    @Test
    @DisplayName("Should find payments by customerId with pagination")
    void shouldFindByCustomerId() {
        UUID customerId = UUID.randomUUID();

        for (int i = 0; i < 5; i++) {
            paymentRepository.save(Payment.builder()
                    .orderId(UUID.randomUUID())
                    .customerId(customerId)
                    .amount(new BigDecimal("100.00"))
                    .currency("INR")
                    .status(PaymentStatus.COMPLETED)
                    .build());
        }

        Page<Payment> page = paymentRepository.findByCustomerId(customerId, PageRequest.of(0, 3));
        assertThat(page.getTotalElements()).isEqualTo(5);
        assertThat(page.getContent()).hasSize(3);
    }

    @Test
    @DisplayName("Should find payment by gateway transaction and order IDs")
    void shouldFindByGatewayIds() {
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();

        Payment payment = Payment.builder()
                .orderId(orderId)
                .customerId(customerId)
                .amount(new BigDecimal("2999.00"))
                .currency("INR")
                .status(PaymentStatus.COMPLETED)
                .gatewayOrderId("order_razorpay_999")
                .gatewayTxnId("pay_razorpay_888")
                .gatewaySignature("sig_razorpay_777")
                .build();

        paymentRepository.save(payment);

        Optional<Payment> byGatewayOrder = paymentRepository.findByGatewayOrderId("order_razorpay_999");
        assertThat(byGatewayOrder).isPresent();
        assertThat(byGatewayOrder.get().getGatewayTxnId()).isEqualTo("pay_razorpay_888");

        Optional<Payment> byGatewayTxn = paymentRepository.findByGatewayTxnId("pay_razorpay_888");
        assertThat(byGatewayTxn).isPresent();
        assertThat(byGatewayTxn.get().getGatewayOrderId()).isEqualTo("order_razorpay_999");
    }

    @Test
    @DisplayName("Should find payments by status with pagination")
    void shouldFindByStatus() {
        UUID customerId = UUID.randomUUID();

        paymentRepository.save(Payment.builder()
                .orderId(UUID.randomUUID())
                .customerId(customerId)
                .amount(new BigDecimal("100.00"))
                .currency("INR")
                .status(PaymentStatus.FAILED)
                .build());

        paymentRepository.save(Payment.builder()
                .orderId(UUID.randomUUID())
                .customerId(customerId)
                .amount(new BigDecimal("200.00"))
                .currency("INR")
                .status(PaymentStatus.COMPLETED)
                .build());

        Page<Payment> completed = paymentRepository.findByStatus(PaymentStatus.COMPLETED, PageRequest.of(0, 10));
        assertThat(completed.getContent()).hasSize(1);
        assertThat(completed.getContent().getFirst().getStatus()).isEqualTo(PaymentStatus.COMPLETED);
    }
}
