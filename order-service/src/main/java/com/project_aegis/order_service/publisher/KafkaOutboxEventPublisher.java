package com.project_aegis.order_service.publisher;

import com.project_aegis.order_service.entity.OutboxEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 * Kafka implementation of {@link OutboxEventPublisher}.
 * <p>
 * Topic derivation: aggregateType "ORDER" → topic "order-events".
 * Message key: aggregateId (ensures per-aggregate ordering within a partition).
 * Header "eventType": downstream consumers can filter without parsing the payload.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaOutboxEventPublisher implements OutboxEventPublisher {

    private static final long SEND_TIMEOUT_SECONDS = 10;

    private final KafkaTemplate<String, String> kafkaTemplate;

    @Override
    public void publish(OutboxEvent event) {
        String topic = deriveTopicName(event.getAggregateType());
        String key = event.getAggregateId().toString();

        ProducerRecord<String, String> record = new ProducerRecord<>(topic, key, event.getPayload());
        record.headers().add(new RecordHeader("eventType",
                event.getEventType().getBytes(StandardCharsets.UTF_8)));
        record.headers().add(new RecordHeader("aggregateId",
                key.getBytes(StandardCharsets.UTF_8)));

        try {
            // Synchronous send — block until broker acknowledges so we can reliably
            // transition to PROCESSED vs FAILED in the same transaction
            kafkaTemplate.send(record).get(SEND_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            log.debug("Published outbox event {} to topic '{}' with key '{}'",
                    event.getId(), topic, key);
        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to publish outbox event %s to Kafka topic '%s'"
                            .formatted(event.getId(), topic), e);
        }
    }

    /**
     * Derives a Kafka topic name from the aggregate type.
     * Example: "ORDER" → "order-events"
     */
    private String deriveTopicName(String aggregateType) {
        return aggregateType.toLowerCase() + "-events";
    }
}
