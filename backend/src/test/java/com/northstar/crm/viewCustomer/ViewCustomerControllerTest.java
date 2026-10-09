package com.northstar.crm.viewCustomer;

import com.northstar.crm.domain.CustomerId;
import com.northstar.crm.domain.CustomerNotFoundException;
import com.northstar.crm.domain.CustomerStatus;
import com.northstar.crm.platform.security.SecurityConfig;
import com.northstar.crm.viewCustomer.dto.CustomerView;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.UUID;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = ViewCustomerController.class,
        properties = {
            "crm.jwt.secret=test-only-secret-not-used-anywhere-real-32+",
            "crm.jwt.ttl=1h",
            "crm.security.agent-password=agent1",
            "crm.security.admin-password=admin1",
            "crm.cors.allowed-origin=http://localhost:4200"
        })
@Import(SecurityConfig.class)
class ViewCustomerControllerTest {

    private static final UUID AMINA = UUID.fromString("5d1c2e0c-3a0d-4b1e-9f9d-8a7d9f4a1c11");
    private static final UUID MISSING = UUID.fromString("8a9c7d2e-0f22-41c7-b1ef-6d1f3ac4d2ce");

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    ViewCustomerService service;

    private static RequestPostProcessor as(String role) {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_" + role));
    }

    @Test
    void agentGetsTheCustomer() throws Exception {
        when(service.get(AMINA)).thenReturn(
            new CustomerView(AMINA, "Amina Khan", "amina.khan@example.com", CustomerStatus.ACTIVE));

        mockMvc.perform(get("/api/v1/customers/" + AMINA).with(as("AGENT")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.customerId").value(AMINA.toString()))
            .andExpect(jsonPath("$.fullName").value("Amina Khan"))
            .andExpect(jsonPath("$.email").value("amina.khan@example.com"))
            .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void adminCanAlsoView() throws Exception {
        when(service.get(AMINA)).thenReturn(
            new CustomerView(AMINA, "Amina Khan", "amina.khan@example.com", CustomerStatus.ACTIVE));

        mockMvc.perform(get("/api/v1/customers/" + AMINA).with(as("ADMIN")))
            .andExpect(status().isOk());
    }

    @Test
    void guestIsRejected() throws Exception {
        mockMvc.perform(get("/api/v1/customers/" + AMINA))
            .andExpect(status().isUnauthorized());

        verifyNoInteractions(service);
    }

    @Test
    void unknownCustomerIs404ProblemDetail() throws Exception {
        when(service.get(MISSING)).thenThrow(new CustomerNotFoundException(new CustomerId(MISSING)));

        mockMvc.perform(get("/api/v1/customers/" + MISSING).with(as("AGENT")))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.title").value("Customer not found"));
    }

    @Test
    void notAUuidIs400() throws Exception {
        mockMvc.perform(get("/api/v1/customers/not-a-uuid").with(as("AGENT")))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }
}
