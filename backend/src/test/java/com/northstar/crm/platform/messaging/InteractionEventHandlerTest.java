package com.northstar.crm.platform.messaging;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/** Duplicate handling is covered end to end by InteractionEventConsumerIntegrationTest; this covers the log-injection guard. */
@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class InteractionEventHandlerTest {

    @Mock
    ProcessedEvents processedEvents;

    @Test
    void lineBreaksInTheCorrelationIdAreNeutralisedBeforeLogging(CapturedOutput output) {
        var event = CustomerInteractionRecordedV1.create(
            UUID.randomUUID(), Instant.parse("2026-10-08T12:00:00Z"), "corr\r\nforged log line", "agent1",
            UUID.randomUUID(), UUID.randomUUID(), "PHONE");
        when(processedEvents.markProcessed(event.eventId())).thenReturn(true);

        new InteractionEventHandler(processedEvents).handle(new ConsumerRecord<>("interactions", 0, 3L, "key", event), event);

        assertThat(output.getAll()).contains("correlationId=corr__forged log line");
    }
}
