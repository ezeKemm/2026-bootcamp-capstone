package com.northstar.crm.showCustomerTimeline;

import com.northstar.crm.domain.CustomerId;
import com.northstar.crm.domain.CustomerNotFoundException;
import com.northstar.crm.domain.InteractionChannel;
import com.northstar.crm.platform.security.SecurityConfig;
import com.northstar.crm.showCustomerTimeline.dto.InteractionRecord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
    controllers = ShowCustomerTimelineController.class,
    properties = {
        "crm.jwt.secret=test-only-secret-not-used-anywhere-real-32+",
        "crm.jwt.ttl=1h",
        "crm.security.agent-password=agent1",
        "crm.security.admin-password=admin1",
        "crm.cors.allowed-origin=http://localhost:4200"
    })
@Import(SecurityConfig.class)
class ShowCustomerTimelineControllerTest {

    private static final UUID AMINA = UUID.fromString("5d1c2e0c-3a0d-4b1e-9f9d-8a7d9f4a1c11");
    private static final String URL = "/api/v1/customers/" + AMINA + "/interactions";

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    ShowCustomerTimelineService service;

    @Test
    void guestGets401() throws Exception {
        mockMvc.perform(get(URL))
            .andExpect(status().isUnauthorized());

        verifyNoInteractions(service);
    }

    @Test
    void agentGetsJsonTimeline() throws Exception {
        UUID interactionId = UUID.fromString("6f1c2b7e-8d3a-4c51-9b2e-0a7d4e9f1c23");
        when(service.timeline(AMINA, new TimelineViewer("agent1", false))).thenReturn(List.of(
            new InteractionRecord(interactionId, AMINA, InteractionChannel.EMAIL, "Sent welcome pack.",
                "agent1", Instant.parse("2026-10-01T14:00:00Z"), "lab-request-001")));

        mockMvc.perform(get(URL).with(asAgent("agent1")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].interactionId").value(interactionId.toString()))
            .andExpect(jsonPath("$[0].channel").value("EMAIL"))
            .andExpect(jsonPath("$[0].actor").value("agent1"))
            .andExpect(jsonPath("$[0].occurredAt").value("2026-10-01T14:00:00Z"));
    }

    @Test
    void adminIsPassedToServiceAsSeeingEverything() throws Exception {
        when(service.timeline(AMINA, new TimelineViewer("admin1", true))).thenReturn(List.of());

        mockMvc.perform(get(URL).with(jwt().jwt(j -> j.subject("admin1"))
                    .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
            .andExpect(status().isOk());

        verify(service).timeline(AMINA, new TimelineViewer("admin1", true));
    }

    @Test
    void unknownCustomerGets404() throws Exception {
        when(service.timeline(AMINA, new TimelineViewer("agent1", false)))
            .thenThrow(new CustomerNotFoundException(new CustomerId(AMINA)));

        mockMvc.perform(get(URL).with(asAgent("agent1")))
            .andExpect(status().isNotFound());
    }

    @Test
    void malformedCustomerIdGets400() throws Exception {
        mockMvc.perform(get("/api/v1/customers/not-a-uuid/interactions").with(asAgent("agent1")))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    private static RequestPostProcessor asAgent(String username) {
        return jwt().jwt(j -> j.subject(username)).authorities(new SimpleGrantedAuthority("ROLE_AGENT"));
    }
}