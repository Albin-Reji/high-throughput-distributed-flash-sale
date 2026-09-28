package com.aegis.payment_service.repository;

import com.aegis.payment_service.entity.Payment;
import com.aegis.payment_service.entity.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    Optional<Payment> findFirstByOrderIdOrderByCreatedAtDesc(UUID orderId);

    List<Payment> findByOrderId(UUID orderId);

    Page<Payment> findByCustomerId(UUID customerId, Pageable pageable);

    Optional<Payment> findByGatewayOrderId(String gatewayOrderId);

    Optional<Payment> findByGatewayTxnId(String gatewayTxnId);

    Page<Payment> findByStatus(PaymentStatus status, Pageable pageable);
}
