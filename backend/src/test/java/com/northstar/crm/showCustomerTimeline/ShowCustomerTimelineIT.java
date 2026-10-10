package com.northstar.crm.showCustomerTimeline;

import com.jayway.jsonpath.JsonPath;
import com.northstar.crm.TestcontainersConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Full-chain test for GET /api/v1/customers/{id}/interactions:
 * real security filters, controller, service, repository, and a real PostgreSQL container.
 * Rows are inserted with SQL so each test controls exactly who recorded what, and when.
 */
@SpringBootTest(properties = "crm.jwt.secret=test-only-secret-not-used-anywhere-real-32+")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ShowCustomerTimelineIntegrationTest {

    // Seeded by Flyway V2: Amina is ACTIVE, Ravi is PROSPECT.
    private static final String AMINA = "5d1c2e0c-3a0d-4b1e-9f9d-8a7d9f4a1c11";
    private static final String RAVI = "7d3b8c73-99f7-4d06-8ce5-f8d8668ed2a1";
    private static final Instant BASE = Instant.parse("2026-10-01T14:00:00Z");

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void startFromKnownState() {
        jdbc.update("delete from customer_interactions");
    }

    /** Admin path: the query with no actor filter returns everyone's entries. */
    @Test
    void admin_seesEveryonesInteractions() throws Exception {
        seedMixedTimeline();
        String token = loginAs("admin1", "admin1");

        mvc.perform(get("/api/v1/customers/{id}/interactions", AMINA)
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(3))
            .andExpect(jsonPath("$[*].summary",
                containsInAnyOrder("Agent1 renewal call", "Agent1 welcome email", "Agent2 chat")));
    }

    /** Agent path: the query filtered by actor hides another agent's work. */
    @Test
    void agent_seesOnlyTheirOwnInteractions() throws Exception {
        seedMixedTimeline();
        String token = loginAs("agent1", "agent1");

        mvc.perform(get("/api/v1/customers/{id}/interactions", AMINA)
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[*].summary",
                containsInAnyOrder("Agent1 renewal call", "Agent1 welcome email")));
    }

    /** Nothing visible is an empty list, not an error. */
    @Test
    void agentWithNoOwnEntries_getsEmptyList() throws Exception {
        seed(AMINA, "CHAT", "Agent2 chat", "agent2", BASE);
        String token = loginAs("agent1", "agent1");

        mvc.perform(get("/api/v1/customers/{id}/interactions", AMINA)
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(0));
    }

    /** The order by clause: newest first, whatever order the rows were inserted in. */
    @Test
    void timeline_isNewestFirst() throws Exception {
        seed(AMINA, "PHONE", "middle", "agent1", BASE.plusSeconds(60));
        seed(AMINA, "PHONE", "oldest", "agent1", BASE);
        seed(AMINA, "PHONE", "newest", "agent1", BASE.plusSeconds(120));
        String token = loginAs("admin1", "admin1");

        mvc.perform(get("/api/v1/customers/{id}/interactions", AMINA)
                .header("Authorization", "Bearer " + token))
            .andExpect(jsonPath("$[*].summary", contains("newest", "middle", "oldest")));
    }

    /** The query filters by customer id. Ravi's row is inserted straight into SQL on purpose,
     *  because the domain rule would block recording for a PROSPECT through the API. */
    @Test
    void timeline_onlyContainsTheRequestedCustomer() throws Exception {
        seed(AMINA, "PHONE", "Amina call", "agent1", BASE);
        seed(RAVI, "PHONE", "Ravi call", "agent1", BASE);
        String token = loginAs("admin1", "admin1");

        mvc.perform(get("/api/v1/customers/{id}/interactions", AMINA)
                .header("Authorization", "Bearer " + token))
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].customerId").value(AMINA));
    }

    /** A real customer with no history is 200 and [], which is different from an unknown customer. */
    @Test
    void customerWithNoInteractions_returnsEmptyList_not404() throws Exception {
        String token = loginAs("admin1", "admin1");

        mvc.perform(get("/api/v1/customers/{id}/interactions", RAVI)
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void unknownCustomer_returns404() throws Exception {
        String token = loginAs("admin1", "admin1");

        mvc.perform(get("/api/v1/customers/{id}/interactions", UUID.randomUUID())
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.title").value("Customer not found"));
    }

    /** The old fake ids (CUS-1001) are not UUIDs. The frontend treats this 400 as "not found". */
    @Test
    void malformedCustomerId_returns400() throws Exception {
        String token = loginAs("admin1", "admin1");

        mvc.perform(get("/api/v1/customers/{id}/interactions", "CUS-1001")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isBadRequest());
    }

    @Test
    void noToken_returns401() throws Exception {
        mvc.perform(get("/api/v1/customers/{id}/interactions", AMINA))
            .andExpect(status().isUnauthorized());
    }

    /** Field names and values match the Interaction schema in openapi.yaml. */
    @Test
    void entry_hasEveryContractField() throws Exception {
        UUID id = seed(AMINA, "EMAIL", "Contract check", "agent1", BASE);
        String token = loginAs("admin1", "admin1");

        mvc.perform(get("/api/v1/customers/{id}/interactions", AMINA)
                .header("Authorization", "Bearer " + token))
            .andExpect(jsonPath("$[0].interactionId").value(id.toString()))
            .andExpect(jsonPath("$[0].customerId").value(AMINA))
            .andExpect(jsonPath("$[0].channel").value("EMAIL"))
            .andExpect(jsonPath("$[0].summary").value("Contract check"))
            .andExpect(jsonPath("$[0].actor").value("agent1"))
            .andExpect(jsonPath("$[0].occurredAt").isNotEmpty())
            .andExpect(jsonPath("$[0].correlationId").value("it-timeline-seed"));
    }

    // ---------- helpers ----------

    /** Two entries by agent1 and one by agent2, all for Amina. */
    private void seedMixedTimeline() {
        seed(AMINA, "PHONE", "Agent1 renewal call", "agent1", BASE);
        seed(AMINA, "EMAIL", "Agent1 welcome email", "agent1", BASE.plusSeconds(60));
        seed(AMINA, "CHAT", "Agent2 chat", "agent2", BASE.plusSeconds(120));
    }

    private UUID seed(String customerId, String channel, String summary, String actor, Instant occurredAt) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
            insert into customer_interactions
              (interaction_id, customer_id, channel, summary, actor, occurred_at, correlation_id)
            values (?, ?, ?, ?, ?, ?, ?)""",
            id, UUID.fromString(customerId), channel, summary, actor,
            Timestamp.from(occurredAt), "it-timeline-seed");
        return id;
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