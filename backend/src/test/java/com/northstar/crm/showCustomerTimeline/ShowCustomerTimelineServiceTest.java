package com.northstar.crm.showCustomerTimeline;

import com.northstar.crm.domain.CustomerNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShowCustomerTimelineServiceTest {

    private static final UUID AMINA = UUID.fromString("5d1c2e0c-3a0d-4b1e-9f9d-8a7d9f4a1c11");
    private static final TimelineViewer AGENT = new TimelineViewer("agent1", false);
    private static final TimelineViewer ADMIN = new TimelineViewer("admin1", true);

    @Mock
    ShowCustomerTimelineRepository repository;

    @Test
    void unknownCustomerThrowsAndNoInteractionsAreQueried() {
        when(repository.customerExists(AMINA)).thenReturn(false);

        assertThatThrownBy(() -> new ShowCustomerTimelineService(repository).timeline(AMINA, ADMIN))
            .isInstanceOf(CustomerNotFoundException.class);

        verify(repository).customerExists(AMINA);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void adminUsesTheFullTimelineQuery() {
        when(repository.customerExists(AMINA)).thenReturn(true);
        when(repository.findTimeline(AMINA)).thenReturn(List.of());

        assertThat(new ShowCustomerTimelineService(repository).timeline(AMINA, ADMIN)).isEmpty();

        verify(repository).findTimeline(AMINA);
        verify(repository, never()).findTimelineByActor(AMINA, "admin1");
    }

    @Test
    void agentUsesTheQueryFilteredByTheirUsername() {
        when(repository.customerExists(AMINA)).thenReturn(true);
        when(repository.findTimelineByActor(AMINA, "agent1")).thenReturn(List.of());

        assertThat(new ShowCustomerTimelineService(repository).timeline(AMINA, AGENT)).isEmpty();

        verify(repository).findTimelineByActor(AMINA, "agent1");
        verify(repository, never()).findTimeline(AMINA);
    }
}