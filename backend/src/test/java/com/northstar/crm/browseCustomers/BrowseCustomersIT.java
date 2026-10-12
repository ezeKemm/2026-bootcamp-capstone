package com.northstar.crm.browseCustomers;

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

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * GET /api/v1/customers against real PostgreSQL: the JPQL ordering, paging, count and status filter,
 * which the service and controller tests only see through mocks.
 * Flyway seeds Amina Khan (ACTIVE) and Ravi Singh (PROSPECT); three more rows are added per test, so the full order is
 * Aaron IT, Amina Khan, Bea IT, Cleo IT, Ravi Singh.
 */
@SpringBootTest(properties = "crm.jwt.secret=test-only-secret-not-used-anywhere-real-32+")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class BrowseCustomersIT {

    private static final String AMINA = "5d1c2e0c-3a0d-4b1e-9f9d-8a7d9f4a1c11";

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void addThreeCustomers() {
        insert("Aaron IT", "PROSPECT");
        insert("Bea IT", "ACTIVE");
        insert("Cleo IT", "PROSPECT");
    }

    @AfterEach
    void removeThem() {
        jdbc.update("delete from customers where full_name like '% IT'");
    }

    @Test
    void agentSeesTheFullRecordOrderedByName() throws Exception {
        mvc.perform(get("/api/v1/customers").header("Authorization", "Bearer " + loginAs("agent1", "agent1")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items[*].fullName",
                contains("Aaron IT", "Amina Khan", "Bea IT", "Cleo IT", "Ravi Singh")))
            .andExpect(jsonPath("$.items[?(@.customerId=='" + AMINA + "')].email").value("amina.khan@example.com"))
            .andExpect(jsonPath("$.totalItems").value(5));
    }

    @Test
    void pagingSlicesTheOrderedListAndReportsTotals() throws Exception {
        String token = loginAs("agent1", "agent1");

        mvc.perform(get("/api/v1/customers").param("page", "0").param("size", "2").header("Authorization", "Bearer " + token))
            .andExpect(jsonPath("$.items[*].fullName", contains("Aaron IT", "Amina Khan")))
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(2))
            .andExpect(jsonPath("$.totalItems").value(5))
            .andExpect(jsonPath("$.totalPages").value(3));

        mvc.perform(get("/api/v1/customers").param("page", "1").param("size", "2").header("Authorization", "Bearer " + token))
            .andExpect(jsonPath("$.items[*].fullName", contains("Bea IT", "Cleo IT")));

        mvc.perform(get("/api/v1/customers").param("page", "2").param("size", "2").header("Authorization", "Bearer " + token))
            .andExpect(jsonPath("$.items[*].fullName", contains("Ravi Singh")));
    }

    @Test
    void statusFilterAndCountAgree() throws Exception {
        mvc.perform(get("/api/v1/customers").param("status", "ACTIVE").header("Authorization", "Bearer " + loginAs("admin1", "admin1")))
            .andExpect(jsonPath("$.items[*].fullName", contains("Amina Khan", "Bea IT")))
            .andExpect(jsonPath("$.items[*].status", everyItem(is("ACTIVE"))))
            .andExpect(jsonPath("$.totalItems").value(2));
    }

    // ---------- helpers ----------

    private void insert(String name, String status) {
        jdbc.update("insert into customers (customer_id, full_name, email, status) values (?, ?, ?, ?)",
            UUID.randomUUID(), name, name.toLowerCase().replace(' ', '.') + "@example.com", status);
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
