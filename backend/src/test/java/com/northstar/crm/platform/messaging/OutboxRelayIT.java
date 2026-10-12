package com.northstar.crm.platform.messaging;

import com.jayway.jsonpath.JsonPath;
import com.northstar.crm.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The transactional outbox end to end: record an interaction through the API, the event row is written in the same
 * transaction, OutboxRelay publishes it to Kafka and marks it published, and the consumer processes it.
 */
@SpringBootTest(properties = {
    "crm.jwt.secret=test-only-secret-not-used-anywhere-real-32+",
    "crm.outbox.poll-ms=200",
    "crm.messaging.retry-interval-ms=10"
})
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class OutboxRelayIT {

    // Seeded by Flyway V2: Amina is ACTIVE, Ravi is PROSPECT.
    private static final String AMINA = "5d1c2e0c-3a0d-4b1e-9f9d-8a7d9f4a1c11";
    private static final String RAVI = "7d3b8c73-99f7-4d06-8ce5-f8d8668ed2a1";

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;

    @MockitoSpyBean
    InteractionEventPublisher publisher;

    @Test
    void recordedInteractionIsPublishedMarkedPublishedAndConsumedOnce() throws Exception {
        String correlationId = "it-outbox-" + UUID.randomUUID();

        record(AMINA, correlationId);

        UUID eventId = awaitEventId(correlationId);
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
            assertThat(publishedAt(eventId)).as("relay marked the row published").isNotNull();
            assertThat(processedCount(eventId)).as("consumer recorded the event once").isEqualTo(1);
        });
    }

    @Test
    void failedPublishLeavesTheRowPendingAndIsRetriedUntilItSucceeds() throws Exception {
        // Kafka is "down": every publish attempt fails.
        doReturn(CompletableFuture.failedFuture(new IllegalStateException("broker down")))
            .when(publisher).publish(any());
        String correlationId = "it-outbox-" + UUID.randomUUID();

        record(AMINA, correlationId);

        UUID eventId = awaitEventId(correlationId);
        verify(publisher, timeout(10_000).atLeast(2)).publish(any());   // the relay keeps retrying
        assertThat(publishedAt(eventId)).as("still pending while the broker is down").isNull();

        // Kafka "comes back": the next retry goes through.
        doCallRealMethod().when(publisher).publish(any());
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() ->
            assertThat(publishedAt(eventId)).isNotNull());
    }

    @Test
    void rejectedInteractionWritesNoOutboxEvent() throws Exception {
        String correlationId = "it-outbox-" + UUID.randomUUID();

        // Ravi is a PROSPECT, so the domain rule rejects the interaction with 422 and nothing may be queued.
        mvc.perform(post("/api/v1/customers/{id}/interactions", RAVI)
                .header("Authorization", "Bearer " + loginAs("agent1", "agent1"))
                .header("X-Correlation-Id", correlationId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"channel\":\"PHONE\",\"summary\":\"Should be rejected.\"}"))
            .andExpect(status().isUnprocessableEntity());

        Integer rows = jdbc.queryForObject(
            "select count(*) from outbox_event where payload like ?", Integer.class, "%" + correlationId + "%");
        assertThat(rows).isZero();
    }

    // ---------- helpers ----------

    private void record(String customerId, String correlationId) throws Exception {
        mvc.perform(post("/api/v1/customers/{id}/interactions", customerId)
                .header("Authorization", "Bearer " + loginAs("agent1", "agent1"))
                .header("X-Correlation-Id", correlationId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"channel\":\"PHONE\",\"summary\":\"Outbox check.\"}"))
            .andExpect(status().isCreated());
    }

    private UUID awaitEventId(String correlationId) {
        return await().atMost(Duration.ofSeconds(10)).until(() ->
            jdbc.queryForList("select event_id from outbox_event where payload like ?", UUID.class,
                "%" + correlationId + "%").stream().findFirst().orElse(null),
            id -> id != null);
    }

    private Object publishedAt(UUID eventId) {
        return jdbc.queryForObject("select published_at from outbox_event where event_id = ?", Object.class, eventId);
    }

    private int processedCount(UUID eventId) {
        return jdbc.queryForObject("select count(*) from processed_event where event_id = ?", Integer.class, eventId);
    }

    private String loginAs(String username, String password) throws Exception {
        String json = mvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$.accessToken");
    }
}
