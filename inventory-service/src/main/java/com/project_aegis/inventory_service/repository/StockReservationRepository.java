package com.project_aegis.inventory_service.repository;

import com.project_aegis.inventory_service.entity.ReservationStatus;
import com.project_aegis.inventory_service.entity.StockReservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StockReservationRepository extends JpaRepository<StockReservation, UUID> {

    List<StockReservation> findAllByOrderId(UUID orderId);

    List<StockReservation> findAllByOrderIdAndStatus(UUID orderId, ReservationStatus status);

    Optional<StockReservation> findByOrderIdAndSkuId(UUID orderId, UUID skuId);

    List<StockReservation> findAllByStatusAndExpiresAtBefore(ReservationStatus status, Instant time);

    boolean existsByOrderIdAndStatus(UUID orderId, ReservationStatus status);
}
