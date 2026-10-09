package com.northstar.crm;

import com.northstar.crm.platform.messaging.CustomerInteractionRecordedV1;
import com.northstar.crm.platform.messaging.InteractionEventPublisher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Import;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@ExtendWith(OutputCaptureExtension.class)
class InteractionEventConsumerIntegrationTest {

    @Autowired
    private InteractionEventPublisher publisher;

    @Test
    void logsEachDeliveryIncludingRepeatedEvents(CapturedOutput output) throws Exception {
        var event = CustomerInteractionRecordedV1.create(
                UUID.randomUUID(), Instant.parse("2026-10-08T12:00:00Z"),
                "consumer-test-" + UUID.randomUUID(), "agent-demo",
                UUID.randomUUID(), UUID.randomUUID(), "PHONE");

        // Two explicit publications must produce two log entries, even for the same event ID.
        publisher.publish(event).get(30, TimeUnit.SECONDS);
        publisher.publish(event).get(30, TimeUnit.SECONDS);

        String expected = "Received interaction event: eventId=" + event.eventId()
                + ", interactionId=" + event.interactionId()
                + ", correlationId=" + event.correlationId();

        await().atMost(Duration.ofSeconds(30)).untilAsserted(() ->
                assertThat(output.getAll().lines()
                        .filter(line -> line.contains(expected))
                        .count()).isEqualTo(2));
    }
}
