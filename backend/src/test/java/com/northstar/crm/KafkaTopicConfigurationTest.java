package com.northstar.crm;

import org.apache.kafka.clients.admin.AdminClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.KafkaAdmin;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class KafkaTopicConfigurationTest {

    @Autowired
    private KafkaAdmin kafkaAdmin;

    @Value("${crm.messaging.interactions-topic}")
    private String interactionsTopic;

    @Value("${crm.messaging.dead-letter-topic}")
    private String deadLetterTopic;

    @Value("${crm.messaging.partitions}")
    private int expectedPartitions;

    @Value("${crm.messaging.replicas}")
    private int expectedReplicas;

    @Test
    void createsInteractionAndDeadLetterTopics() throws Exception {
        try (var admin = AdminClient.create(
                kafkaAdmin.getConfigurationProperties())) {

            var topicNames = List.of(
                    interactionsTopic,
                    deadLetterTopic
            );

            var descriptions = admin.describeTopics(topicNames)
                    .allTopicNames()
                    .get(30, TimeUnit.SECONDS);

            for (String name : topicNames) {
                var topic = descriptions.get(name);

                assertNotNull(topic, "Missing topic: " + name);
                assertEquals(
                        expectedPartitions,
                        topic.partitions().size(),
                        "Incorrect partition count: " + name
                );

                for (var partition : topic.partitions()) {
                    assertEquals(
                            expectedReplicas,
                            partition.replicas().size(),
                            "Incorrect replica count: " + name
                    );
                }
            }
        }
    }
}