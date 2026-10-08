package com.northstar.crm.browsePublicCustomers;

import com.northstar.crm.browsePublicCustomers.dto.PublicCustomerSummaryPage;
import com.northstar.crm.domain.CustomerStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class BrowsePublicCustomersServiceTest {
    @Mock
    BrowsePublicCustomersRepository repository;

    @Test
    void returnsRequestedPageMappedToDto() {
        // 1. Arrange
        PublicCustomerSummary amina = new PublicCustomerSummary("Amina Khan", CustomerStatus.ACTIVE);
        PublicCustomerSummary ravi = new PublicCustomerSummary("Ravi Singh", CustomerStatus.PROSPECT);
        when(repository.findRecordSummaries(any(), any()))
            .thenReturn(new PageImpl<>(List.of(amina, ravi), PageRequest.of(1, 2), 5));

        // 2. Act
        PublicCustomerSummaryPage page = new BrowsePublicCustomersService(repository).list(1,2, null);

        // 3. Assert
        ArgumentCaptor<Pageable> requested = ArgumentCaptor.forClass(Pageable.class);
        verify(repository).findRecordSummaries(any(), requested.capture());
        assertThat(requested.getValue().getPageNumber()).isEqualTo(1);
        assertThat(requested.getValue().getPageSize()).isEqualTo(2);
        assertThat(page.items()).containsExactly(amina, ravi);
        assertThat(page.page()).isEqualTo(1);
        assertThat(page.size()).isEqualTo(2);
        assertThat(page.totalItems()).isEqualTo(5);
        assertThat(page.totalPages()).isEqualTo(3);
    }

    @Test
    void withoutStatusFilterReturnsAllStatuses() {
        // 1. Arrange
        when(repository.findRecordSummaries(any(), any()))
            .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        // 2. Act
        new BrowsePublicCustomersService(repository).list(0, 20, null);

        // 3. Assert
        ArgumentCaptor<List<CustomerStatus>> requestedStatuses = ArgumentCaptor.forClass(List.class);
        verify(repository).findRecordSummaries(requestedStatuses.capture(), any());
        assertThat(requestedStatuses.getValue()).containsExactlyInAnyOrder(CustomerStatus.values());
    }

    @Test
    void statusFilterBecomesAnExactStatus() {
        // 1. Arrange
        when(repository.findRecordSummaries(any(), any()))
            .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        // 2. Act
        new BrowsePublicCustomersService(repository).list(0, 20, CustomerStatus.ACTIVE);

        // 3. Assert
        ArgumentCaptor<List<CustomerStatus>> requestedStatuses = ArgumentCaptor.forClass(List.class);
        verify(repository).findRecordSummaries(requestedStatuses.capture(), any());
        assertThat(requestedStatuses.getValue()).containsExactly(CustomerStatus.ACTIVE);
    }
}
