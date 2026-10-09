package com.northstar.crm;

import com.northstar.crm.platform.messaging.CustomerInteractionRecordedV1;
import com.northstar.crm.platform.messaging.InteractionEventConsumer;
import com.northstar.crm.platform.messaging.InteractionEventPublisher;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.testcontainers.kafka.KafkaContainer;

import java.nio.charset.StandardCharsets;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

@SpringBootTest(properties = {
        "crm.messaging.retry-interval-ms=10",
        "crm.messaging.consumer-group=crm-failure-test-${random.uuid}"
})
@Import(TestcontainersConfiguration.class)
@ExtendWith(OutputCaptureExtension.class)
class InteractionEventConsumerFailureIntegrationTest {

    @Autowired
    private InteractionEventPublisher publisher;

    @Autowired
    private KafkaContainer kafka;

    @MockitoSpyBean
    private InteractionEventConsumer listener;

    @Value("${crm.messaging.interactions-topic}")
    private String topic;

    @Value("${crm.messaging.dead-letter-topic}")
    private String deadLetterTopic;

    @Test
    void malformedJsonGoesToDeadLetterTopicWithOriginalBytes(CapturedOutput output) throws Exception {
        String key = UUID.randomUUID().toString();
        String payload = "{invalid-json}";
        publishRaw(key, payload);

        var record = awaitDeadLetter(key);
        assertThat(record.value()).isEqualTo(payload);
        assertThat(record.partition()).isZero();
        assertThat(record.headers().lastHeader(KafkaHeaders.DLT_ORIGINAL_TOPIC)).isNotNull();
        assertThat(record.headers().lastHeader(KafkaHeaders.DLT_EXCEPTION_FQCN)).isNotNull();
        long originalOffset = ByteBuffer.wrap(record.headers()
                .lastHeader(KafkaHeaders.DLT_ORIGINAL_OFFSET).value()).getLong();
        String expectedLog = "Recovered Kafka event to DLT: originalTopic=" + topic
                + ", partition=0, offset=" + originalOffset
                + ", failureType=DeserializationException, deadLetterTopic=" + deadLetterTopic;
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            assertThat(output.getAll()).contains(expectedLog);
            assertThat(output.getAll().lines()
                    .filter(line -> line.contains("Recovered Kafka event to DLT:")))
                    .noneMatch(line -> line.contains(payload));
        });
    }

    @Test
    void unsupportedVersionGoesToDeadLetterTopicWithOriginalPayload() throws Exception {
        var event = event();
        String payload;
        try (var serializer = new JacksonJsonSerializer<CustomerInteractionRecordedV1>()) {
            payload = new String(serializer.serialize(topic, event), StandardCharsets.UTF_8)
                    .replace("\"eventVersion\":1", "\"eventVersion\":99");
        }
        assertThat(payload).contains("\"eventVersion\":99");
        publishRaw(event.customerId().toString(), payload);

        assertThat(awaitDeadLetter(event.customerId().toString()).value()).isEqualTo(payload);
    }

    @Test
    void exhaustedProcessingFailureGoesToDeadLetterAfterThreeAttempts() throws Exception {
        var event = event();
        var attempts = failProcessing(event, false);
        publisher.publish(event).get(30, TimeUnit.SECONDS);

        var record = awaitDeadLetter(event.customerId().toString());
        assertThat(attempts.get()).isEqualTo(3);
        assertThat(record.value()).contains(event.eventId().toString());
        assertThat(record.value()).startsWith("{");
    }

    @Test
    void transientFailureSucceedsOnThirdAttemptWithoutDeadLetter() throws Exception {
        var event = event();
        var attempts = failProcessing(event, true);
        publisher.publish(event).get(30, TimeUnit.SECONDS);

        await().atMost(Duration.ofSeconds(30)).untilAsserted(() ->
                assertThat(attempts.get()).isEqualTo(3));

        try (var consumer = deadLetterConsumer()) {
            long deadline = System.nanoTime() + Duration.ofSeconds(3).toNanos();
            while (System.nanoTime() < deadline) {
                for (var record : consumer.poll(Duration.ofMillis(200))) {
                    assertThat(record.key()).isNotEqualTo(event.customerId().toString());
                }
            }
        }
    }

    private AtomicInteger failProcessing(CustomerInteractionRecordedV1 event, boolean recover) {
        var attempts = new AtomicInteger();
        doAnswer(invocation -> {
            ConsumerRecord<String, CustomerInteractionRecordedV1> record = invocation.getArgument(0);
            if (record.value() != null && record.value().eventId().equals(event.eventId())) {
                int attempt = attempts.incrementAndGet();
                if (!recover || attempt < 3) {
                    throw new IllegalStateException("Controlled processing failure");
                }
            }
            return invocation.callRealMethod();
        }).when(listener).consume(any());
        return attempts;
    }

    private CustomerInteractionRecordedV1 event() {
        return CustomerInteractionRecordedV1.create(
                UUID.randomUUID(), Instant.parse("2026-10-08T12:00:00Z"),
                "failure-test-" + UUID.randomUUID(), "agent-demo",
                UUID.randomUUID(), UUID.randomUUID(), "PHONE");
    }

    private void publishRaw(String key, String payload) throws Exception {
        Map<String, Object> properties = Map.of(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers(),
                ProducerConfig.ACKS_CONFIG, "all");
        try (var producer = new KafkaProducer<String, String>(properties,
                new StringSerializer(), new StringSerializer())) {
            producer.send(new ProducerRecord<>(topic, key, payload)).get(30, TimeUnit.SECONDS);
        }
    }

    private KafkaConsumer<String, String> deadLetterConsumer() {
        Map<String, Object> properties = Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers(),
                ConsumerConfig.GROUP_ID_CONFIG, "dlt-test-" + UUID.randomUUID(),
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest",
                ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        var consumer = new KafkaConsumer<String, String>(properties,
                new StringDeserializer(), new StringDeserializer());
        consumer.subscribe(List.of(deadLetterTopic));
        return consumer;
    }

    private ConsumerRecord<String, String> awaitDeadLetter(String key) {
        try (var consumer = deadLetterConsumer()) {
            long deadline = System.nanoTime() + Duration.ofSeconds(30).toNanos();
            while (System.nanoTime() < deadline) {
                for (var record : consumer.poll(Duration.ofMillis(500))) {
                    if (key.equals(record.key())) {
                        return record;
                    }
                }
            }
        }
        return fail("No dead-letter record received for key " + key);
    }
}
