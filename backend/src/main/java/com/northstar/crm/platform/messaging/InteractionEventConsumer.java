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

        log.info("Received interaction event: eventId={}, interactionId={}, correlationId={}, "
                        + "topic={}, partition={}, offset={}",
                event.eventId(), event.interactionId(),
                event.correlationId().replace('\r', '_').replace('\n', '_'),
                record.topic(), record.partition(), record.offset());
    }
}
