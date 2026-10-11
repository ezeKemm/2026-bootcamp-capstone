package com.northstar.crm.activateCustomer;

import com.jayway.jsonpath.JsonPath;
import com.northstar.crm.TestcontainersConfiguration;
import org.junit.jupiter.api.AfterEach;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Full-chain test for POST /api/v1/customers/{id}/activate:
 * real security filters, real controller, service, repository, and a real PostgreSQL container.
 */
@SpringBootTest(properties = "crm.jwt.secret=test-only-secret-not-used-anywhere-real-32+")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ActivateCustomerIT {

    // Seeded by Flyway V2: Amina is ACTIVE, Ravi is PROSPECT.
    private static final String AMINA = "5d1c2e0c-3a0d-4b1e-9f9d-8a7d9f4a1c11";
    private static final String RAVI = "7d3b8c73-99f7-4d06-8ce5-f8d8668ed2a1";

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;

    /** Other tests rely on Ravi being a PROSPECT, so put him back before and after every test. */
    @BeforeEach
    void startWithRaviAsProspect() {
        resetRaviToProspect();
    }

    @AfterEach
    void leaveRaviAsProspect() {
        resetRaviToProspect();
    }

    @Test
    void agentActivatesProspect_returns200_andStatusIsSavedAsActive() throws Exception {
        String token = loginAs("agent1", "agent1");

        mvc.perform(post("/api/v1/customers/{id}/activate", RAVI)
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.customerId").value(RAVI))
            .andExpect(jsonPath("$.status").value("ACTIVE"));

        assertThat(statusOf(RAVI)).isEqualTo("ACTIVE");
    }

    @Test
    void adminActivatesProspect_returns200() throws Exception {
        String token = loginAs("admin1", "admin1");

        mvc.perform(post("/api/v1/customers/{id}/activate", RAVI)
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void alreadyActiveCustomer_returns422_andStaysActive() throws Exception {
        String token = loginAs("agent1", "agent1");

        mvc.perform(post("/api/v1/customers/{id}/activate", AMINA)
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isUnprocessableContent())
            .andExpect(jsonPath("$.detail").value("Only prospects can be activated."));

        assertThat(statusOf(AMINA)).isEqualTo("ACTIVE");
    }

    @Test
    void unknownCustomer_returns404() throws Exception {
        String token = loginAs("agent1", "agent1");

        mvc.perform(post("/api/v1/customers/{id}/activate", UUID.randomUUID())
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isNotFound());
    }

    @Test
    void noToken_returns401_andNothingChanges() throws Exception {
        mvc.perform(post("/api/v1/customers/{id}/activate", RAVI))
            .andExpect(status().isUnauthorized());

        assertThat(statusOf(RAVI)).isEqualTo("PROSPECT");
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

    private void resetRaviToProspect() {
        jdbc.update("update customers set status = 'PROSPECT' where customer_id = ?", UUID.fromString(RAVI));
    }

    private String statusOf(String customerId) {
        return jdbc.queryForObject(
            "select status from customers where customer_id = ?", String.class, UUID.fromString(customerId));
    }
}
