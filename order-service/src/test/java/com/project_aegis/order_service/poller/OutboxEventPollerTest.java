package com.project_aegis.order_service.poller;

import com.project_aegis.order_service.entity.OutboxEvent;
import com.project_aegis.order_service.entity.OutboxStatus;
import com.project_aegis.order_service.publisher.OutboxEventPublisher;
import com.project_aegis.order_service.repository.OutboxEventRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxEventPollerTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private OutboxEventPublisher outboxEventPublisher;

    @InjectMocks
    private OutboxEventPoller poller;

    @Test
    @DisplayName("Should fetch pending events, mark PROCESSING, publish, then mark PROCESSED")
    void pollAndPublish_happyPath() {
        // Arrange
        ReflectionTestUtils.setField(poller, "batchSize", 50);

        OutboxEvent event = OutboxEvent.builder()
                .id(UUID.randomUUID())
                .aggregateType("ORDER")
                .aggregateId(UUID.randomUUID())
                .eventType("ORDER_CREATED")
                .payload("{\"orderId\": \"123\"}")
                .status(OutboxStatus.PENDING)
                .build();

        when(outboxEventRepository.findPendingForUpdate(50))
                .thenReturn(List.of(event));

        // Act
        poller.pollAndPublish();

        // Assert — event was marked PROCESSING first, then PROCESSED
        assertThat(event.getStatus()).isEqualTo(OutboxStatus.PROCESSED);
        assertThat(event.getProcessedAt()).isNotNull();

        // Verify publish was called
        verify(outboxEventPublisher).publish(event);

        // Verify saveAll was called twice: once for PROCESSING, once for final status
        verify(outboxEventRepository, times(2)).saveAll(List.of(event));
        verify(outboxEventRepository).flush();
    }

    @Test
    @DisplayName("Should mark event as FAILED when publisher throws exception")
    void pollAndPublish_publishFails_marksEventFailed() {
        // Arrange
        ReflectionTestUtils.setField(poller, "batchSize", 50);

        OutboxEvent event = OutboxEvent.builder()
                .id(UUID.randomUUID())
                .aggregateType("ORDER")
                .aggregateId(UUID.randomUUID())
                .eventType("ORDER_CREATED")
                .payload("{}")
                .status(OutboxStatus.PENDING)
                .build();

        when(outboxEventRepository.findPendingForUpdate(50))
                .thenReturn(List.of(event));
        doThrow(new RuntimeException("Kafka unavailable"))
                .when(outboxEventPublisher).publish(event);

        // Act
        poller.pollAndPublish();

        // Assert — event should be FAILED, not PROCESSED
        assertThat(event.getStatus()).isEqualTo(OutboxStatus.FAILED);
        assertThat(event.getProcessedAt()).isNotNull();

        verify(outboxEventRepository, times(2)).saveAll(List.of(event));
    }

    @Test
    @DisplayName("Should do nothing when no pending events exist")
    void pollAndPublish_noPendingEvents_doesNothing() {
        // Arrange
        ReflectionTestUtils.setField(poller, "batchSize", 50);

        when(outboxEventRepository.findPendingForUpdate(50))
                .thenReturn(Collections.emptyList());

        // Act
        poller.pollAndPublish();

        // Assert — no publish, no save
        verify(outboxEventPublisher, never()).publish(any());
        verify(outboxEventRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("Should handle mixed success and failure in a batch")
    void pollAndPublish_mixedBatch_handlesIndividually() {
        // Arrange
        ReflectionTestUtils.setField(poller, "batchSize", 50);

        OutboxEvent successEvent = OutboxEvent.builder()
                .id(UUID.randomUUID())
                .aggregateType("ORDER")
                .aggregateId(UUID.randomUUID())
                .eventType("ORDER_CREATED")
                .payload("{}")
                .status(OutboxStatus.PENDING)
                .build();

        OutboxEvent failEvent = OutboxEvent.builder()
                .id(UUID.randomUUID())
                .aggregateType("ORDER")
                .aggregateId(UUID.randomUUID())
                .eventType("ORDER_CANCELLED")
                .payload("{}")
                .status(OutboxStatus.PENDING)
                .build();

        List<OutboxEvent> events = List.of(successEvent, failEvent);

        when(outboxEventRepository.findPendingForUpdate(50))
                .thenReturn(events);
        doNothing().when(outboxEventPublisher).publish(successEvent);
        doThrow(new RuntimeException("Broker down"))
                .when(outboxEventPublisher).publish(failEvent);

        // Act
        poller.pollAndPublish();

        // Assert — each event has the correct terminal status
        assertThat(successEvent.getStatus()).isEqualTo(OutboxStatus.PROCESSED);
        assertThat(failEvent.getStatus()).isEqualTo(OutboxStatus.FAILED);

        verify(outboxEventPublisher, times(2)).publish(any());
    }
}
