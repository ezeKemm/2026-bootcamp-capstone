package com.northstar.crm.platform.messaging;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
class ProcessedEvents {
    private final JdbcClient jbdc;
    private final String consumerGroup;

    ProcessedEvents(JdbcClient jbdc, @Value("${crm.messaging.consume-group}") String consumerGroup) {
        this.jbdc = jbdc;
        this.consumerGroup = consumerGroup;
    }

    /** Returns true for any event it sees for the first time, false for a duplicate. */
    boolean markProcessed(UUID eventId) {
        return jbdc.sql("""
            INSERT INTO processed_event (consume_group, event_id)
            VALUE (:consumerGroup, :eventId)
            ON CONFLICT DO NOTHING
            """)
            .param("consumerGroup", consumerGroup)
            .param("eventId", eventId)
            .update() == 1;
    }
}
