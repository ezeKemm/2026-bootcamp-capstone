package com.northstar.crm.showCustomerTimeline;

import com.northstar.crm.domain.CustomerId;
import com.northstar.crm.domain.CustomerInteraction;
import com.northstar.crm.domain.CustomerNotFoundException;
import com.northstar.crm.showCustomerTimeline.dto.InteractionRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
class ShowCustomerTimelineService {

    private final ShowCustomerTimelineRepository repo;

    ShowCustomerTimelineService(ShowCustomerTimelineRepository repo) {
        this.repo = repo;
    }

    @Transactional(readOnly = true)
    List<InteractionRecord> timeline(UUID customerId, TimelineViewer viewer) {
        if (!repo.customerExists(customerId)) {
            throw new CustomerNotFoundException(new CustomerId(customerId));
        }

        List<CustomerInteraction> visible = viewer.seesEveryInteraction()
            ? repo.findTimeline(customerId)                              // ADMIN
            : repo.findTimelineByActor(customerId, viewer.username());   // AGENT

        return visible.stream()
            .map(ShowCustomerTimelineService::toRecord)
            .toList();
    }

    private static InteractionRecord toRecord(CustomerInteraction i) {
        return new InteractionRecord(
            i.getInteractionId().value(), i.getCustomerId().value(),
            i.getChannel(), i.getSummary(), i.getActor(),
            i.getOccurredAt(), i.getCorrelationId());
    }
}