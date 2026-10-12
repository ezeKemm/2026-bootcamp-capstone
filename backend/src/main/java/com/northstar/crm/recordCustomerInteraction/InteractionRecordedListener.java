package com.northstar.crm.recordCustomerInteraction;

import com.northstar.crm.domain.CustomerInteraction;
import com.northstar.crm.platform.messaging.CustomerInteractionRecordedV1;
import com.northstar.crm.platform.messaging.Outbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.UUID;

@Component
public class InteractionRecordedListener {
    private final static Logger log = LoggerFactory.getLogger(InteractionRecordedListener.class);

    private final Outbox outbox;

    InteractionRecordedListener(Outbox outbox) {
        this.outbox = outbox;
    }

    /**
     * Events are persisted within the transaction in the outbox table
     * so a publisher failure does not swallow the event
     */
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    void on (InteractionRecorded record) {
        CustomerInteraction i = record.interaction();
        var event = CustomerInteractionRecordedV1.create(
            UUID.randomUUID(),
            i.getOccurredAt(),
            i.getCorrelationId(),
            i.getActor(),
            i.getCustomerId().value(),
            i.getInteractionId().value(),
            i.getChannel().name());
            outbox.add(event);
    }
}
