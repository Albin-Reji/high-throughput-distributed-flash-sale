package com.project_aegis.order_service.publisher;

import com.project_aegis.order_service.entity.OutboxEvent;

/**
 * Strategy interface for publishing outbox events to an external message broker.
 * Implementations must throw an exception if publishing fails so the poller
 * can mark the event as FAILED.
 */
public interface OutboxEventPublisher {

    /**
     * Publishes the given outbox event to the message broker.
     *
     * @param event the outbox event to publish
     * @throws RuntimeException if publishing fails
     */
    void publish(OutboxEvent event);
}
