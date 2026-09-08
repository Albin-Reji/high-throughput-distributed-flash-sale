package com.project_aegis.order_service.poller;

import com.project_aegis.order_service.entity.OutboxEvent;
import com.project_aegis.order_service.entity.OutboxStatus;
import com.project_aegis.order_service.publisher.OutboxEventPublisher;
import com.project_aegis.order_service.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Scheduled poller that picks up PENDING outbox events using
 * {@code SELECT ... FOR UPDATE SKIP LOCKED}, publishes them via
 * {@link OutboxEventPublisher}, and marks them PROCESSED or FAILED.
 * <p>
 * The fixed delay of 1 second ensures there is always a 1-second gap
 * between the end of one poll cycle and the start of the next,
 * preventing overlap even if a batch takes a while to process.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxEventPoller {

    private final OutboxEventRepository outboxEventRepository;
    private final OutboxEventPublisher outboxEventPublisher;

    @Value("${outbox.poller.batch-size:50}")
    private int batchSize;

    /**
     * Polls for pending outbox events and publishes them.
     * <p>
     * Runs inside a transaction so the FOR UPDATE SKIP LOCKED row locks
     * are held until all events in the batch are processed.
     */
    @Scheduled(fixedDelay = 1000)
    @Transactional
    public void pollAndPublish() {
        List<OutboxEvent> pendingEvents = outboxEventRepository.findPendingForUpdate(batchSize);

        if (pendingEvents.isEmpty()) {
            return;
        }

        log.info("Outbox poller picked up {} pending event(s)", pendingEvents.size());

        // Mark all as PROCESSING and flush so the status is visible
        // (prevents re-fetch by another poller instance even after lock release)
        for (OutboxEvent event : pendingEvents) {
            event.setStatus(OutboxStatus.PROCESSING);
        }
        outboxEventRepository.saveAll(pendingEvents);
        outboxEventRepository.flush();

        // Publish each event individually
        for (OutboxEvent event : pendingEvents) {
            try {
                outboxEventPublisher.publish(event);
                event.setStatus(OutboxStatus.PROCESSED);
                event.setProcessedAt(Instant.now());
                log.debug("Outbox event {} published successfully", event.getId());
            } catch (Exception e) {
                event.setStatus(OutboxStatus.FAILED);
                event.setProcessedAt(Instant.now());
                log.error("Failed to publish outbox event {}: {}", event.getId(), e.getMessage(), e);
            }
        }

        outboxEventRepository.saveAll(pendingEvents);
    }
}
