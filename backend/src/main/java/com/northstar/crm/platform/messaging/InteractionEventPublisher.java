package com.northstar.crm.platform.messaging;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
public class InteractionEventPublisher {

    private final KafkaTemplate<
            String, CustomerInteractionRecordedV1> kafkaTemplate;

    private final String topic;

    public InteractionEventPublisher(
            KafkaTemplate<
                    String, CustomerInteractionRecordedV1> kafkaTemplate,
            @Value("${crm.messaging.interactions-topic}") String topic
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    public CompletableFuture<Void> publish(
            CustomerInteractionRecordedV1 event
    ) {
        try {
            return kafkaTemplate
                    .send(topic, event.customerId().toString(), event)
                    .thenApply(result -> (Void) null);
        } catch (RuntimeException exception) {
            return CompletableFuture.failedFuture(exception);
        }
    }
}