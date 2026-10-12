package com.northstar.crm.browsePublicCustomers;

import com.northstar.crm.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * GET /api/v1/public/customers against real PostgreSQL, with no token. The privacy rule is that a guest sees names
 * and statuses only. Uses the Flyway seed: Amina Khan (ACTIVE) and Ravi Singh (PROSPECT).
 */
@SpringBootTest(properties = "crm.jwt.secret=test-only-secret-not-used-anywhere-real-32+")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class BrowsePublicCustomersIT {

    @Autowired MockMvc mvc;

    @Test
    void guestSeesNamesAndStatusesOnly_neverEmailOrId() throws Exception {
        String body = mvc.perform(get("/api/v1/public/customers"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items[*].fullName", contains("Amina Khan", "Ravi Singh")))
            .andExpect(jsonPath("$.items[0].email").doesNotExist())
            .andExpect(jsonPath("$.items[0].customerId").doesNotExist())
            .andReturn().getResponse().getContentAsString();

        assertThat(body).doesNotContain("@").doesNotContain("5d1c2e0c").doesNotContain("7d3b8c73");
    }

    @Test
    void statusFilterReturnsOnlyThatStatus() throws Exception {
        mvc.perform(get("/api/v1/public/customers").param("status", "PROSPECT"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items[*].fullName", contains("Ravi Singh")))
            .andExpect(jsonPath("$.totalItems").value(1));
    }
}
