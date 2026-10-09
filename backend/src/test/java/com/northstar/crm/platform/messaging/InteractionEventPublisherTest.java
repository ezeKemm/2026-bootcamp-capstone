package com.northstar.crm.platform.messaging;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InteractionEventPublisherTest {

    private static final String TOPIC = "crm.customer.interactions.v1";

    @Mock
    private KafkaTemplate<String, CustomerInteractionRecordedV1> template;

    private InteractionEventPublisher publisher;
    private CustomerInteractionRecordedV1 event;

    @BeforeEach
    void setUp() {
        publisher = new InteractionEventPublisher(template, TOPIC);
        event = CustomerInteractionRecordedV1.create(
                UUID.randomUUID(), Instant.parse("2026-10-08T12:00:00Z"),
                "lab-request-001", "agent-demo",
                UUID.randomUUID(), UUID.randomUUID(), "PHONE");
    }

    @Test
    void propagatesImmediateSendFailureThroughReturnedFuture() {
        var failure = new IllegalStateException("Send failed");
        when(template.send(TOPIC, event.customerId().toString(), event))
                .thenThrow(failure);

        var result = publisher.publish(event);

        var thrown = assertThrows(ExecutionException.class,
                () -> result.get(5, TimeUnit.SECONDS));
        assertSame(failure, thrown.getCause());
    }

    @Test
    void propagatesFailureReportedAfterPublishReturns() {
        var pending = new CompletableFuture<
                SendResult<String, CustomerInteractionRecordedV1>>();
        when(template.send(TOPIC, event.customerId().toString(), event))
                .thenReturn(pending);

        var result = publisher.publish(event);
        assertFalse(result.isDone(), "Publication must wait for Kafka's result");

        var failure = new IllegalStateException("Delivery failed");
        pending.completeExceptionally(failure);

        var thrown = assertThrows(ExecutionException.class,
                () -> result.get(5, TimeUnit.SECONDS));
        assertSame(failure, thrown.getCause());
    }
}
