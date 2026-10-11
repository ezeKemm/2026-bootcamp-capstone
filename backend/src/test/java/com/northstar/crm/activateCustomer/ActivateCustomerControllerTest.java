package com.northstar.crm.activateCustomer;

import com.northstar.crm.activateCustomer.dto.ActivatedCustomer;
import com.northstar.crm.domain.CustomerId;
import com.northstar.crm.domain.CustomerNotFoundException;
import com.northstar.crm.domain.CustomerStatus;
import com.northstar.crm.domain.DomainException;
import com.northstar.crm.platform.security.SecurityConfig;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = ActivateCustomerController.class,
        properties = {
            "crm.jwt.secret=test-only-secret-not-used-anywhere-real-32+",
            "crm.jwt.ttl=1h",
            "crm.security.agent-password=agent1",
            "crm.security.admin-password=admin1",
            "crm.cors.allowed-origin=http://localhost:4200"
        })
@Import(SecurityConfig.class)
class ActivateCustomerControllerTest {

    private static final UUID RAVI = UUID.fromString("7d3b8c73-99f7-4d06-8ce5-f8d8668ed2a1");
    private static final UUID MISSING = UUID.fromString("8a9c7d2e-0f22-41c7-b1ef-6d1f3ac4d2ce");

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    ActivateCustomerService service;

    private static RequestPostProcessor as(String role) {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_" + role));
    }

    private static ActivatedCustomer raviActive() {
        return new ActivatedCustomer(RAVI, "Ravi Singh", "ravi.singh@example.com", CustomerStatus.ACTIVE);
    }

    @Test
    void agentActivatesAProspect() throws Exception {
        when(service.activate(RAVI)).thenReturn(raviActive());

        mockMvc.perform(post("/api/v1/customers/" + RAVI + "/activate").with(as("AGENT")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.customerId").value(RAVI.toString()))
            .andExpect(jsonPath("$.fullName").value("Ravi Singh"))
            .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void adminCanAlsoActivate() throws Exception {
        when(service.activate(RAVI)).thenReturn(raviActive());

        mockMvc.perform(post("/api/v1/customers/" + RAVI + "/activate").with(as("ADMIN")))
            .andExpect(status().isOk());
    }

    @Test
    void guestIsRejected() throws Exception {
        mockMvc.perform(post("/api/v1/customers/" + RAVI + "/activate"))
            .andExpect(status().isUnauthorized());

        verifyNoInteractions(service);
    }

    @Test
    void unknownCustomerIs404() throws Exception {
        when(service.activate(MISSING)).thenThrow(new CustomerNotFoundException(new CustomerId(MISSING)));

        mockMvc.perform(post("/api/v1/customers/" + MISSING + "/activate").with(as("AGENT")))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.title").value("Customer not found"));
    }

    @Test
    void alreadyActiveIs422() throws Exception {
        when(service.activate(RAVI)).thenThrow(new DomainException("Only prospects can be activated."));

        mockMvc.perform(post("/api/v1/customers/" + RAVI + "/activate").with(as("AGENT")))
            .andExpect(status().is(422))
            .andExpect(jsonPath("$.detail").value("Only prospects can be activated."));
    }

    @Test
    void notAUuidIs400() throws Exception {
        mockMvc.perform(post("/api/v1/customers/not-a-uuid/activate").with(as("AGENT")))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }
}
