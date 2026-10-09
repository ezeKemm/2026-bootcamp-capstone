package com.northstar.crm;

import com.northstar.crm.platform.messaging.CustomerInteractionRecordedV1;
import com.northstar.crm.platform.messaging.InteractionEventPublisher;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;
import org.testcontainers.kafka.KafkaContainer;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class InteractionEventPublisherIntegrationTest {

    @Autowired
    private InteractionEventPublisher publisher;

    @Autowired
    private KafkaContainer kafka;

    @Value("${crm.messaging.interactions-topic}")
    private String topic;

    @Test
    void publishesEventWithCustomerIdAsKey() throws Exception {
        var event = CustomerInteractionRecordedV1.create(
                UUID.randomUUID(),
                Instant.parse("2026-10-08T12:00:00Z"),
                "lab-request-001",
                "agent-demo",
                UUID.randomUUID(),
                UUID.randomUUID(),
                "PHONE"
        );

        Map<String, Object> properties = Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                kafka.getBootstrapServers(),
                ConsumerConfig.GROUP_ID_CONFIG,
                "publisher-test-" + UUID.randomUUID(),
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
                "earliest",
                ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG,
                false
        );

        var valueDeserializer = new JacksonJsonDeserializer<>(
                CustomerInteractionRecordedV1.class,
                false
        );

        try (var consumer =
                     new KafkaConsumer<String, CustomerInteractionRecordedV1>(
                             properties,
                             new StringDeserializer(),
                             valueDeserializer
                     )) {

            consumer.subscribe(List.of(topic));

            // Wait for Kafka to acknowledge publication.
            publisher.publish(event).get(30, TimeUnit.SECONDS);

            long deadline = System.nanoTime()
                    + Duration.ofSeconds(30).toNanos();

            while (System.nanoTime() < deadline) {
                var records = consumer.poll(Duration.ofSeconds(1));

                for (var record : records) {
                    if (event.eventId().equals(record.value().eventId())) {
                        assertEquals(topic, record.topic());
                        assertEquals(
                                event.customerId().toString(),
                                record.key()
                        );
                        assertEquals(event, record.value());
                        return;
                    }
                }
            }

            fail("Published event was not received within 30 seconds");
        }
    }
}