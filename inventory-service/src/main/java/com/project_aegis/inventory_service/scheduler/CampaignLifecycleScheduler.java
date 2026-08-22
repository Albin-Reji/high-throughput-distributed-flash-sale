package com.project_aegis.inventory_service.scheduler;

import com.project_aegis.inventory_service.entity.FlashCampaign;
import com.project_aegis.inventory_service.entity.FlashCampaignStatus;
import com.project_aegis.inventory_service.entity.Inventory;
import com.project_aegis.inventory_service.entity.ReservationStatus;
import com.project_aegis.inventory_service.entity.StockReservation;
import com.project_aegis.inventory_service.repository.FlashCampaignRepository;
import com.project_aegis.inventory_service.repository.InventoryRepository;
import com.project_aegis.inventory_service.repository.StockReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Slf4j
@Configuration
@EnableScheduling
@RequiredArgsConstructor
public class CampaignLifecycleScheduler {

    private final FlashCampaignRepository flashCampaignRepository;
    private final StockReservationRepository stockReservationRepository;
    private final InventoryRepository inventoryRepository;

    /**
     * Periodically checks and transitions campaign lifecycle states:
     * - PUBLISHED -> ACTIVE when startTime has arrived
     * - ACTIVE -> ENDED when endTime has passed
     */
    @Scheduled(fixedRateString = "${campaign.scheduler.interval-ms:15000}")
    @Transactional
    public void processCampaignStateTransitions() {
        Instant now = Instant.now();

        // 1. Auto-activate PUBLISHED campaigns whose start time has arrived
        List<FlashCampaign> publishedCampaigns = flashCampaignRepository
                .findAllByStatusIn(List.of(FlashCampaignStatus.PUBLISHED), org.springframework.data.domain.Pageable.unpaged())
                .getContent();

        for (FlashCampaign campaign : publishedCampaigns) {
            if (!campaign.getStartTime().isAfter(now)) {
                campaign.setStatus(FlashCampaignStatus.ACTIVE);
                flashCampaignRepository.save(campaign);
                log.info("Scheduler automatically activated campaign id={}, name={}",
                        campaign.getId(), campaign.getName());
            }
        }

        // 2. Auto-end ACTIVE campaigns whose end time has passed
        List<FlashCampaign> activeCampaigns = flashCampaignRepository
                .findAllByStatusIn(List.of(FlashCampaignStatus.ACTIVE), org.springframework.data.domain.Pageable.unpaged())
                .getContent();

        for (FlashCampaign campaign : activeCampaigns) {
            if (campaign.getEndTime().isBefore(now)) {
                campaign.setStatus(FlashCampaignStatus.ENDED);
                flashCampaignRepository.save(campaign);
                log.info("Scheduler automatically ended campaign id={}, name={}",
                        campaign.getId(), campaign.getName());
            }
        }
    }

    /**
     * Periodically cleans up expired stock reservations and returns stock to available pool.
     */
    @Scheduled(fixedRateString = "${reservation.cleanup.interval-ms:30000}")
    @Transactional
    public void cleanupExpiredReservations() {
        Instant now = Instant.now();
        List<StockReservation> expiredReservations = stockReservationRepository
                .findAllByStatusAndExpiresAtBefore(ReservationStatus.RESERVED, now);

        if (!expiredReservations.isEmpty()) {
            log.info("Cleaning up {} expired stock reservation(s)", expiredReservations.size());

            for (StockReservation reservation : expiredReservations) {
                Optional<Inventory> inventoryOpt = inventoryRepository.findBySkuId(reservation.getSkuId());
                if (inventoryOpt.isPresent()) {
                    Inventory inventory = inventoryOpt.get();
                    inventory.setAvailableQuantity(inventory.getAvailableQuantity() + reservation.getQuantity());
                    inventoryRepository.save(inventory);
                }

                reservation.setStatus(ReservationStatus.CANCELLED);
                stockReservationRepository.save(reservation);
            }
        }
    }
}
