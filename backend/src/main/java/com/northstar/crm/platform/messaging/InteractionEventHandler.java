package com.northstar.crm.platform.messaging;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
class InteractionEventHandler {
    private static final Logger log = LoggerFactory.getLogger(InteractionEventHandler.class);
    private final ProcessedEvents processedEvents;

    InteractionEventHandler(ProcessedEvents processedEvents) {
        this.processedEvents = processedEvents;
    }

    @Transactional
    void handle(ConsumerRecord<String, CustomerInteractionRecordedV1> record, CustomerInteractionRecordedV1 event) {
        String correlatedId = event.correlationId().replace('\r', '_').replace('\n', '_');

        // Log duplicate and skip
        if (!processedEvents.markProcessed(event.eventId())) {
            log.info("Skipping duplicate interaction event: eventId={}, correlationId={}, topicId={}, partition={}, offset={}",
                event.eventId(), correlatedId, record.topic(), record.partition(), record.offset());
            return;
        }

        log.info("Received interaction event: eventId={}, interactionId={}, correlationId={}, "
                + "topic={}, partition={}, offset={}",
            event.eventId(), event.interactionId(), correlatedId,
            record.topic(), record.partition(), record.offset());
    }

}
