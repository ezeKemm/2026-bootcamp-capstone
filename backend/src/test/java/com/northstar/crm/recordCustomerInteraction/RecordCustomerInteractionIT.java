package com.northstar.crm.recordCustomerInteraction;

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

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Full-chain test for POST /api/v1/customers/{id}/interactions:
 * real security filters, real controller, service, repository, and a real PostgreSQL container.
 */
@SpringBootTest(properties = "crm.jwt.secret=test-only-secret-not-used-anywhere-real-32+")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class RecordCustomerInteractionIT {

    // Seeded by Flyway V2: Amina is ACTIVE, Ravi is PROSPECT.
    private static final String AMINA = "5d1c2e0c-3a0d-4b1e-9f9d-8a7d9f4a1c11";
    private static final String RAVI = "7d3b8c73-99f7-4d06-8ce5-f8d8668ed2a1";

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void startFromKnownState() {
        jdbc.update("delete from customer_interactions");   // customers stay as Flyway seeded them
    }

    @Test
    void agentRecordsInteraction_returns201_andRowIsInDatabase() throws Exception {
        String token = loginAs("agent1", "agent1");

        mvc.perform(post("/api/v1/customers/{id}/interactions", AMINA)
                .header("Authorization", "Bearer " + token)
                .header("X-Correlation-Id", "it-record-001")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body("PHONE", "Called about renewal.")))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.customerId").value(AMINA))
            .andExpect(jsonPath("$.actor").value("agent1"))          // from the JWT, not the request
            .andExpect(jsonPath("$.channel").value("PHONE"))
            .andExpect(jsonPath("$.correlationId").value("it-record-001"));

        String actor = jdbc.queryForObject(
            "select actor from customer_interactions where correlation_id = ?", String.class, "it-record-001");
        assertThat(actor).isEqualTo("agent1");
    }

    @Test
    void recordedInteraction_appearsOnTheTimeline() throws Exception {
        String token = loginAs("agent1", "agent1");

        mvc.perform(post("/api/v1/customers/{id}/interactions", AMINA)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body("EMAIL", "Sent welcome pack.")))
            .andExpect(status().isCreated());

        mvc.perform(get("/api/v1/customers/{id}/interactions", AMINA)
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].summary").value("Sent welcome pack."))
            .andExpect(jsonPath("$[0].actor").value("agent1"));
    }

    @Test
    void prospectCustomer_returns422_andNothingIsSaved() throws Exception {
        String token = loginAs("agent1", "agent1");

        mvc.perform(post("/api/v1/customers/{id}/interactions", RAVI)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body("PHONE", "Should be rejected.")))
            .andExpect(status().isUnprocessableContent())
            .andExpect(jsonPath("$.title").value("Business rule violated"));

        assertThat(rowCount()).isZero();
    }

    @Test
    void unknownCustomer_returns404() throws Exception {
        String token = loginAs("agent1", "agent1");

        mvc.perform(post("/api/v1/customers/{id}/interactions", UUID.randomUUID())
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body("PHONE", "Nobody home.")))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.title").value("Customer not found"));
    }

    @Test
    void blankSummary_returns400_withFieldError() throws Exception {
        String token = loginAs("agent1", "agent1");

        mvc.perform(post("/api/v1/customers/{id}/interactions", AMINA)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body("PHONE", "   ")))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors[0].field").value("summary"));

        assertThat(rowCount()).isZero();
    }

    @Test
    void unknownChannel_returns400() throws Exception {
        String token = loginAs("agent1", "agent1");

        mvc.perform(post("/api/v1/customers/{id}/interactions", AMINA)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body("FAX", "Old school.")))
            .andExpect(status().isBadRequest());
    }

    @Test
    void noToken_returns401_andNothingIsSaved() throws Exception {
        mvc.perform(post("/api/v1/customers/{id}/interactions", AMINA)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body("PHONE", "No token.")))
            .andExpect(status().isUnauthorized());

        assertThat(rowCount()).isZero();
    }

    // ---------- helpers ----------

    private String loginAs(String username, String password) throws Exception {
        String json = mvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$.accessToken");
    }

    private static String body(String channel, String summary) {
        return "{\"channel\":\"" + channel + "\",\"summary\":\"" + summary + "\"}";
    }

    private int rowCount() {
        return jdbc.queryForObject("select count(*) from customer_interactions", Integer.class);
    }
}