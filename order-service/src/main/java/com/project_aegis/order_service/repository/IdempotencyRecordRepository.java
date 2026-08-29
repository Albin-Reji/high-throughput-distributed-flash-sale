package com.project_aegis.order_service.repository;

import com.project_aegis.order_service.entity.IdempotencyRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface IdempotencyRecordRepository extends JpaRepository<IdempotencyRecord, String> {

    Optional<IdempotencyRecord> findByIdempotencyKeyAndCustomerId(String idempotencyKey, UUID customerId);
}
