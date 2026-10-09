package com.northstar.crm.recordCustomerInteraction;

import com.northstar.crm.domain.Customer;
import com.northstar.crm.domain.CustomerInteraction;
import com.northstar.crm.platform.messaging.CustomerInteractionRecordedV1;
import com.northstar.crm.platform.messaging.InteractionEventPublisher;
import com.northstar.crm.showCustomerTimeline.dto.InteractionRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.UUID;

@Component
public class InteractionRecordedListener {
    private final static Logger log = LoggerFactory.getLogger(InteractionRecordedListener.class);
    private final InteractionEventPublisher publisher;

    InteractionRecordedListener(InteractionEventPublisher publisher) {
        this.publisher = publisher;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void on (InteractionRecorded record) {
        CustomerInteraction i = record.interaction();
        try {
            var event = CustomerInteractionRecordedV1.create(
                UUID.randomUUID(),
                i.getOccurredAt(),
                i.getCorrelationId(),
                i.getActor(),
                i.getCustomerId().value(),
                i.getInteractionId().value(),
                i.getChannel().name());
            publisher.publish(event).whenComplete((ok, error) -> {
                if (error != null) {
                    logFailure(i, error);
                }
            });
        } catch (RuntimeException err) {
            logFailure(i, err);
        }
    }

    private void logFailure(CustomerInteraction i, Throwable error) {
        log.error("Failed to publish interaction event: interactionId={}, correlationId={}, error={}",
            i.getInteractionId().value(),
            String.valueOf(i.getCorrelationId()).replace('\r', '_').replace('\n', '_'),
            error.toString());
    }
}
