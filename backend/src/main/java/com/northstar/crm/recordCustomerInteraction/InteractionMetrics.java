package com.northstar.crm.recordCustomerInteraction;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Counts interactions that were actually saved: crm.interaction.recorded{channel}.
 * Runs after the database commit, so a rolled-back record is never counted.
 *
 * Tags stay low-cardinality (channel has four values). Never tag by customer, interaction or actor.
 * Rejected requests are already counted by http.server.requests (status / outcome tags).
 */
@Component
class InteractionMetrics {

    static final String RECORDED = "crm.interaction.recorded";

    private final MeterRegistry registry;

    InteractionMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void on(InteractionRecorded recorded) {
        Counter.builder(RECORDED)
            .description("Customer interactions saved")
            .tag("channel", recorded.interaction().getChannel().name())
            .register(registry)
            .increment();
    }
}
