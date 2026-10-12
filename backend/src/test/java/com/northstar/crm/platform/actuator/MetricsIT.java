package com.northstar.crm.platform.actuator;

import com.jayway.jsonpath.JsonPath;
import com.northstar.crm.TestcontainersConfiguration;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Metrics for NFR-6 (response time) and operations: /actuator/metrics is ADMIN only,
 * request timings carry p95 per endpoint, saved interactions are counted, and no metric
 * is tagged with a customer ID.
 */
@SpringBootTest(properties = "crm.jwt.secret=test-only-secret-not-used-anywhere-real-32+")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class MetricsIT {

    // Seeded by Flyway V2.
    private static final String AMINA = "5d1c2e0c-3a0d-4b1e-9f9d-8a7d9f4a1c11";
    private static final String CUSTOMER_URI = "/api/v1/customers/{customerId}";

    @Autowired MockMvc mvc;
    @Autowired MeterRegistry registry;
    @Autowired JdbcTemplate jdbc;

    @AfterEach
    void noInteractions() {
        jdbc.update("delete from customer_interactions");
    }

    @Test
    void metricsEndpoint_rejectsGuests() throws Exception {
        mvc.perform(get("/actuator/metrics"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void metricsEndpoint_isForbiddenForAgents() throws Exception {
        mvc.perform(get("/actuator/metrics")
                .header("Authorization", "Bearer " + loginAs("agent1", "agent1")))
            .andExpect(status().isForbidden());
    }

    @Test
    void viewingACustomer_isTimedWithP95_perEndpointNotPerCustomer() throws Exception {
        mvc.perform(get("/api/v1/customers/{id}", AMINA)
                .header("Authorization", "Bearer " + loginAs("agent1", "agent1")))
            .andExpect(status().isOk());

        String admin = "Bearer " + loginAs("admin1", "admin1");

        mvc.perform(get("/actuator/metrics/http.server.requests.percentile")
                .header("Authorization", admin)
                .param("tag", "uri:" + CUSTOMER_URI)
                .param("tag", "method:GET")
                .param("tag", "phi:0.95"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.measurements[0].statistic").value("VALUE"));

        String requests = mvc.perform(get("/actuator/metrics/http.server.requests")
                .header("Authorization", admin))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        assertThat(requests)
            .as("uri tag is the URL template, never a real customer ID")
            .contains(CUSTOMER_URI)
            .doesNotContain(AMINA);
    }

    @Test
    void recordingAnInteraction_incrementsCounterByChannel() throws Exception {
        double before = recordedCount("PHONE");

        mvc.perform(post("/api/v1/customers/{id}/interactions", AMINA)
                .header("Authorization", "Bearer " + loginAs("agent1", "agent1"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"channel\":\"PHONE\",\"summary\":\"Metrics check.\"}"))
            .andExpect(status().isCreated());

        assertThat(recordedCount("PHONE")).isEqualTo(before + 1);

        mvc.perform(get("/actuator/metrics/crm.interaction.recorded")
                .header("Authorization", "Bearer " + loginAs("admin1", "admin1"))
                .param("tag", "channel:PHONE"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.measurements[0].statistic").value("COUNT"));
    }

    @Test
    void rejectedRecord_isNotCountedAsRecorded() throws Exception {
        double before = recordedCount("EMAIL");

        // Blank summary -> 400, nothing saved.
        mvc.perform(post("/api/v1/customers/{id}/interactions", AMINA)
                .header("Authorization", "Bearer " + loginAs("agent1", "agent1"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"channel\":\"EMAIL\",\"summary\":\"\"}"))
            .andExpect(status().isBadRequest());

        assertThat(recordedCount("EMAIL")).isEqualTo(before);
    }

    private double recordedCount(String channel) {
        Counter counter = registry.find("crm.interaction.recorded").tag("channel", channel).counter();
        return counter == null ? 0 : counter.count();
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
