package com.northstar.crm.browseCustomers;

import com.northstar.crm.browseCustomers.dto.CustomerRecord;
import com.northstar.crm.browseCustomers.dto.CustomerRecordPage;
import com.northstar.crm.domain.Customer;
import com.northstar.crm.domain.CustomerStatus;
import org.apache.kafka.common.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@ExtendWith(MockitoExtension.class)
public class BrowseCustomersServiceTest {
    @Mock
    BrowseCustomersRepository repository;

    @Test
    void returnsRequestedPageWithDetailsAndDto() {
        Customer amina = Customer.registerProspect("Amina Khan", "amina.khan@example.com");
        amina.activate(); // set to ACTIVE
        Customer ravi = Customer.registerProspect("Ravi Singh", "ravi.singh@example.com");

        when(repository.findRecords(any(), any())).thenReturn(new PageImpl<>(List.of(amina, ravi), PageRequest.of(1, 2), 5));

        CustomerRecordPage page = new BrowseCustomersService(repository).list(1, 2, null);

        ArgumentCaptor<Pageable> requested = ArgumentCaptor.forClass(Pageable.class);
        verify(repository).findRecords(any(), requested.capture());
        assertThat(requested.getValue().getPageNumber()).isEqualTo(1);
        assertThat(requested.getValue().getPageSize()).isEqualTo(2);
        assertThat(page.items()).containsExactly(
            new CustomerRecord(amina.getCustomerId().value(), amina.getFullName(), amina.getEmail(), amina.getStatus()),
            new CustomerRecord(ravi.getCustomerId().value(), ravi.getFullName(), ravi.getEmail(), ravi.getStatus())
        );
        assertThat(page.page()).isEqualTo(1);
        assertThat(page.size()).isEqualTo(2);
        assertThat(page.totalItems()).isEqualTo(5);
        assertThat(page.totalPages()).isEqualTo(3);
    }

    @Test
    void withoutAStatusFilterEveryStatusMatches() {
        when(repository.findRecords(any(), any())).thenReturn(new PageImpl<>(List.of()));

        new BrowseCustomersService(repository).list(0, 20, null);


        ArgumentCaptor<List<CustomerStatus>> requestedStatuses = ArgumentCaptor.forClass(List.class);
        verify(repository).findRecords(requestedStatuses.capture(), any());
        assertThat(requestedStatuses.getValue()).containsExactlyInAnyOrder(CustomerStatus.values());
    }

    @Test
    void statusFilterBecomesAnExactStatus() {
        when(repository.findRecords(any(), any())).thenReturn(new PageImpl<>(List.of()));

        new BrowseCustomersService(repository).list(0, 20, CustomerStatus.ACTIVE);

        ArgumentCaptor<List<CustomerStatus>> requestedStatuses = ArgumentCaptor.forClass(List.class);
        verify(repository).findRecords(requestedStatuses.capture(), any());
        assertThat(requestedStatuses.getValue()).containsExactly(CustomerStatus.ACTIVE);
    }

}
