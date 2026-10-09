package com.northstar.crm.recordCustomerInteraction;

import com.northstar.crm.domain.CustomerId;
import com.northstar.crm.domain.InteractionChannel;
import com.northstar.crm.platform.security.SecurityConfig;
import com.northstar.crm.recordCustomerInteraction.dto.RecordInteractionRequest;
import com.northstar.crm.recordCustomerInteraction.dto.RecordInteractionResponse;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
    controllers = RecordCustomerInteractionController.class,
    properties = "crm.jwt.secret=NABhwbS9vqriUk0ny6UQipitY8gLWx60qpydNkhSuxXyCwFJaX9FtZxKtn1fcdan"
)
@Import(SecurityConfig.class)
class RecordCustomerInteractionControllerTest {
    @Autowired private MockMvc mockMvc;

    @MockitoBean private RecordCustomerInteractionService service;

    @ParameterizedTest
    @CsvSource({"agent1,AGENT", "admin1,ADMIN"})
    void createsInteractionForAuthenticatedRoles(String username, String role) throws Exception {
        UUID customerId = UUID.randomUUID();
        UUID interactionId = UUID.randomUUID();

        var response = new RecordInteractionResponse(
            interactionId,
            customerId,
            username,
            InteractionChannel.EMAIL,
            "Sent onboarding email.",
            Instant.now(),
            "lab-request-001"
        );

        when(service.record(
            eq(new CustomerId(customerId)),
            any(RecordInteractionRequest.class),
            eq(username),
            eq("lab-request-001")
        )).thenReturn(response);

        mockMvc.perform(post("/api/v1/customers/{customerId}/interactions", customerId)
                .with(user(username).roles(role))
                .header("X-Correlation-Id", "lab-request-001")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"channel\":\"EMAIL\",\"summary\":\"Sent onboarding email.\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.interactionId").value(interactionId.toString()))
            .andExpect(jsonPath("$.customerId").value(customerId.toString()))
            .andExpect(jsonPath("$.actor").value(username))
            .andExpect(jsonPath("$.channel").value("EMAIL"))
            .andExpect(jsonPath("$.summary").value("Sent onboarding email."))
            .andExpect(jsonPath("$.correlationId").value("lab-request-001"));

        verify(service).record(
            eq(new CustomerId(customerId)),
            any(RecordInteractionRequest.class),
            eq(username),
            eq("lab-request-001")
        );
    }

    @Test
    void blankSummaryReturns400() throws Exception {
        UUID customerId = UUID.randomUUID();
        mockMvc.perform(post("/api/v1/customers/{customerId}/interactions", customerId)
                .with(user("agent1").roles("AGENT"))
                .header("X-Correlation-Id", "lab-request-001")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                {"channel":"PHONE","summary":"   "}"""))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors[0].field").value("summary"));

        verifyNoInteractions(service);
    }
}
