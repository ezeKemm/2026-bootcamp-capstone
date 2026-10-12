package com.northstar.crm.viewCustomer;

import com.jayway.jsonpath.JsonPath;
import com.northstar.crm.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "crm.jwt.secret=test-only-secret-not-used-anywhere-real-32+")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ViewCustomerIT {

    private static final String AMINA = "5d1c2e0c-3a0d-4b1e-9f9d-8a7d9f4a1c11";

    @Autowired MockMvc mvc;

    @Test
    void agentSeesTheSeededActiveCustomer() throws Exception {
        mvc.perform(get("/api/v1/customers/{id}", AMINA).header("Authorization", "Bearer " + loginAs("agent1", "agent1")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.customerId").value(AMINA))
            .andExpect(jsonPath("$.fullName").value("Amina Khan"))
            .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void unknownCustomerReturns404() throws Exception {
        mvc.perform(get("/api/v1/customers/{id}", UUID.randomUUID()).header("Authorization", "Bearer " + loginAs("agent1", "agent1")))
            .andExpect(status().isNotFound());
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
