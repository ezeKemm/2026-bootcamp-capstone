package com.northstar.crm.browsePublicCustomers;

import com.northstar.crm.browsePublicCustomers.dto.PublicCustomerSummaryPage;
import com.northstar.crm.domain.CustomerStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;


@WebMvcTest(BrowsePublicCustomersController.class)
@AutoConfigureMockMvc(addFilters = false)
public class BrowsePublicCustomersControllerTest {
    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    BrowsePublicCustomersService service;

    @Test
    void guestCanBrowseWithoutToken() throws Exception {
        // Arrange: Mock service call
        when(service.list(0, 20, null)).thenReturn(new PublicCustomerSummaryPage(List.of(
            new PublicCustomerSummary("Amina Khan", CustomerStatus.ACTIVE)), 0, 20, 1, 1));
        // Act
        mockMvc.perform(get("/api/v1/public/customers"))
            // Assert
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items[0].fullName").value("Amina Khan"))
            .andExpect(jsonPath("$.items[0].status").value("ACTIVE"))
            // Summary does not expose sensitive information without token
            .andExpect(jsonPath("$.items[0].customerId").doesNotExist())
            .andExpect(jsonPath("$.items[0].email").doesNotExist())
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(20))
            .andExpect(jsonPath("$.totalItems").value(1))
            .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    void pageAndSizeAreReadFromTheQuery() throws Exception {
        when(service.list(2, 50, null)).thenReturn(
            new PublicCustomerSummaryPage(List.of(), 2, 50, 0, 0));
        mockMvc.perform(get("/api/v1/public/customers?page=2&size=50"))
            .andExpect(status().isOk());

        verify(service).list(2, 50, null);
    }

    @Test
    void statusFilterIsReadFromTheQuery() throws Exception {
        when(service.list(0, 20, CustomerStatus.ACTIVE)).thenReturn(
            new PublicCustomerSummaryPage(List.of(), 0, 20, 0, 0));
        mockMvc.perform(get("/api/v1/public/customers?status=ACTIVE"))
            .andExpect(status().isOk());

        verify(service).list(0, 20, CustomerStatus.ACTIVE);
    }

    @ParameterizedTest
    @CsvSource({"size,0", "size,101", "page,-1", "status,BOGUS"})
    void invalidParametersReturnBadRequest(String param, String value) throws Exception {
        mockMvc.perform(get("/api/v1/public/customers").queryParam(param, value))
            .andExpect(status().isBadRequest());
    }
}
