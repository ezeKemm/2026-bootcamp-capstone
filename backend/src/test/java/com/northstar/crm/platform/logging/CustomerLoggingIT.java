package com.northstar.crm.platform.logging;

import com.jayway.jsonpath.JsonPath;
import com.northstar.crm.TestcontainersConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Logging contract (docs/logging.md): service log lines carry corr, cust and op,
 * business rejects are logged as WARN with a reason code, and no names, emails or
 * interaction summaries ever reach the logs.
 */
@SpringBootTest(properties = "crm.jwt.secret=test-only-secret-not-used-anywhere-real-32+")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@ExtendWith(OutputCaptureExtension.class)
class CustomerLoggingIT {

    // Seeded by Flyway V2.
    private static final String AMINA = "5d1c2e0c-3a0d-4b1e-9f9d-8a7d9f4a1c11";
    private static final String CORRELATION_ID = "lab-request-001";
    private static final String SUMMARY = "Asked about mortgage rates.";

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    @AfterEach
    void noInteractions() {
        jdbc.update("delete from customer_interactions");
    }

    @Test
    void recordAndView_logIdsAndOperation_withoutPii(CapturedOutput output) throws Exception {
        String token = loginAs("agent1", "agent1");

        mvc.perform(post("/api/v1/customers/{id}/interactions", AMINA)
                .header("Authorization", "Bearer " + token)
                .header(CorrelationFilter.HEADER_NAME, CORRELATION_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"channel\":\"PHONE\",\"summary\":\"" + SUMMARY + "\"}"))
            .andExpect(status().isCreated());

        // The response body has Amina's name and email; the logs must not.
        mvc.perform(get("/api/v1/customers/{id}", AMINA)
                .header("Authorization", "Bearer " + token)
                .header(CorrelationFilter.HEADER_NAME, CORRELATION_ID))
            .andExpect(status().isOk());

        assertThat(lineContaining(output, "recorded interaction id="))
            .contains("corr=" + CORRELATION_ID, "cust=" + AMINA, "op=record_interaction", "channel=PHONE");
        assertThat(lineContaining(output, "viewed customer id="))
            .contains("corr=" + CORRELATION_ID, "cust=" + AMINA, "op=view", "status=ACTIVE");

        String logs = output.getAll();
        assertThat(logs)
            .doesNotContain("Amina")
            .doesNotContain("Khan")
            .doesNotContainIgnoringCase("amina.khan@example.com")
            .doesNotContain(SUMMARY)
            .doesNotContain(token);
    }

    @Test
    void businessRuleReject_isLoggedAsWarnWithReason(CapturedOutput output) throws Exception {
        String token = loginAs("admin1", "admin1");

        // Amina is already ACTIVE, so activating her breaks the "only prospects" rule.
        mvc.perform(post("/api/v1/customers/{id}/activate", AMINA)
                .header("Authorization", "Bearer " + token)
                .header(CorrelationFilter.HEADER_NAME, CORRELATION_ID))
            .andExpect(status().isUnprocessableContent());

        assertThat(lineContaining(output, "reason=business_rule"))
            .contains("WARN", "corr=" + CORRELATION_ID, "cust=" + AMINA, "op=activate", "status=422");
    }

    private static String lineContaining(CapturedOutput output, String text) {
        return output.getAll().lines()
            .filter(line -> line.contains(text))
            .findFirst()
            .orElseThrow(() -> new AssertionError("No log line contains: " + text));
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
