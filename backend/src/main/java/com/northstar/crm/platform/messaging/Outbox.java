package com.northstar.crm.platform.messaging;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
public class Outbox {
    private final JdbcClient jbdc;
    private final JsonMapper json;

    Outbox(JdbcClient jbdc, JsonMapper json) {
        this.jbdc = jbdc;
        this.json = json;
    }

    public void add(CustomerInteractionRecordedV1 event) {
        jbdc.sql("""
                INSERT INTO outbox_event (event_id, customer_id, payload)
                VALUES (?, ?, ?)
                """)
            .params(event.eventId(), event.customerId(), json.writeValueAsString(event))
            .update();
    }
}
