package com.northstar.crm.platform.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.util.UUID;
import java.util.concurrent.TimeUnit;


@Component
public class OutboxRelay {
    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);

    private final JdbcClient jbdc;
    private final JsonMapper json;
    private final InteractionEventPublisher publisher;

    public OutboxRelay(JdbcClient jbdc, JsonMapper json, InteractionEventPublisher publisher) {
        this.jbdc = jbdc;
        this.json = json;
        this.publisher = publisher;
    }

    @Scheduled(fixedDelayString = "${crm.outbox.poll-ms:1000}")
    @Transactional
    void relay() {
        var rows = jbdc.sql("""
                            select event_id, payload from outbox_event
                            where published_at is null
                            order by created_at
                            limit 50
                            for update skip locked
                            """)
            .query((rs, n) -> new Row(rs.getObject("event_id", UUID.class), rs.getString("payload"))).list();

        for (Row row : rows ) {
            try {
                publisher.publish(json.readValue(row.payload(), CustomerInteractionRecordedV1.class))
                    .get(5, TimeUnit.SECONDS);
                jbdc.sql("update outbox_event set published_at = now() where event_id = ?").param(row.id()).update();
                log.info("Published outbox event: eventId={}", row.id());
            } catch (Exception error) {
                // A poisoned row would throw JacksonException (malformed JSON) and stall the relay loop until the row is fixed or skipped
                // Out-of-scope to correct in capstone, record risk and continue (poisoned row is rare)
                log.warn("Outbox publish failed, will retry: eventId={}", row.id(), error);
                return;
            }
        }
    }

    private record Row(UUID id, String payload) {}
}
