package com.northstar.crm.browseCustomers;

import com.northstar.crm.browseCustomers.dto.CustomerRecord;
import com.northstar.crm.browseCustomers.dto.CustomerRecordPage;
import com.northstar.crm.domain.CustomerStatus;
import org.apache.kafka.common.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@WebMvcTest(BrowseCustomersController.class)
@Import(SecurityConfig.class)
public class BrowseCustomersControllerTest {

    private static final UUID AMINA = UUID.fromString("5d1c2e0c-3a0d-4b1e-9f9d-8a7d9f4a1c11");

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    BrowseCustomersService service;

    @Test
    void guestIsRejected() throws Exception {
        mockMvc.perform(get("/api/v1/customers"))
            .andExpect(status().isUnauthorized());

        verifyNoInteractions(service);
    }

    @Test
    void authenticatedUserSeesFullRecord() throws Exception {
        when(service.list(0, 20, null)).thenReturn(new CustomerRecordPage(List.of(
            new CustomerRecord(AMINA, "Amina Khan", "amina.khan@example.com", CustomerStatus.ACTIVE)), 0, 20, 1, 1));

        mockMvc.perform(get("/api/v1/customers").with(user("agent")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items[0].customerId").value(AMINA.toString()))
            .andExpect(jsonPath("$.items[0].fullName").value("Amina Khan"))
            .andExpect(jsonPath("$.items[0].email").value("amina.khan@example.com"))
            .andExpect(jsonPath("$.items[0].status").value("ACTIVE"))
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(20))
            .andExpect(jsonPath("$.totalItems").value(1))
            .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    void pageAndSizeAreReadFromTheQuery() throws Exception {
        when(service.list(2, 50, CustomerStatus.PROSPECT)).thenReturn(
            new CustomerRecordPage(List.of(), 2, 50, 0, 0));

        mockMvc.perform(get("/api/v1/customers").with(user("agent"))
                    .param("page", "2") .param("size", "50") .param("status", "PROSPECT"))
            .andExpect(status().isOk());

        verify(service).list(2, 50, CustomerStatus.PROSPECT);
    }

    @ParameterizedTest
    @CsvSource({"size,0", "size,101", "page,-1", "status,BOGUS"})
    void invalidParametersReturnBadRequest(String param, String value) throws Exception {
        mockMvc.perform(get("/api/v1/customers").with(user("agent"))
                    .param(param, value))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }
}
