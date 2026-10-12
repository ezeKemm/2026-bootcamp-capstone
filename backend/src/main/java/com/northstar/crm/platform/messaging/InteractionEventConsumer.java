package com.northstar.crm.platform.messaging;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class InteractionEventConsumer {

    private static final Logger log =
            LoggerFactory.getLogger(InteractionEventConsumer.class);
    private final InteractionEventHandler handler;

    public InteractionEventConsumer(InteractionEventHandler handler) {
        this.handler = handler;
    }

    @KafkaListener(
            topics = "${crm.messaging.interactions-topic}",
            groupId = "${crm.messaging.consumer-group}"
    )
    public void consume(ConsumerRecord<String, CustomerInteractionRecordedV1> record) {
        var event = record.value();
        if (event == null) {
            log.warn("Received empty interaction event: topic={}, partition={}, offset={}",
                    record.topic(), record.partition(), record.offset());
            return;
        }

        // Logs or dedupes
        handler.handle(record, event);
    }
}
