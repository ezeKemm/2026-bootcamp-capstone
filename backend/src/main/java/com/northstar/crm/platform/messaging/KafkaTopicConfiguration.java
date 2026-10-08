package com.northstar.crm.platform.messaging;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration(proxyBeanMethods = false)
public class KafkaTopicConfiguration {

    private final String interactionsTopic;
    private final String deadLetterTopic;
    private final int partitions;
    private final int replicas;

    public KafkaTopicConfiguration(
            @Value("${crm.messaging.interactions-topic}")
            String interactionsTopic,
            @Value("${crm.messaging.dead-letter-topic}")
            String deadLetterTopic,
            @Value("${crm.messaging.partitions}")
            int partitions,
            @Value("${crm.messaging.replicas}")
            int replicas
    ) {
        this.interactionsTopic = interactionsTopic;
        this.deadLetterTopic = deadLetterTopic;
        this.partitions = partitions;
        this.replicas = replicas;
    }

    @Bean
    public NewTopic customerInteractionsTopic() {
        return TopicBuilder.name(interactionsTopic)
                .partitions(partitions)
                .replicas(replicas)
                .build();
    }

    @Bean
    public NewTopic customerInteractionsDeadLetterTopic() {
        return TopicBuilder.name(deadLetterTopic)
                .partitions(partitions)
                .replicas(replicas)
                .build();
    }
}