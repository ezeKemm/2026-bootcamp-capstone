package com.northstar.crm.showCustomerTimeline;

import com.northstar.crm.showCustomerTimeline.dto.InteractionRecord;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/customers")
class ShowCustomerTimelineController {

    private final ShowCustomerTimelineService service;

    ShowCustomerTimelineController(ShowCustomerTimelineService service) {
        this.service = service;
    }

    @GetMapping("/{customerId}/interactions")
    public List<InteractionRecord> timeline(@PathVariable UUID customerId, Authentication authentication) {
        return service.timeline(customerId, TimelineViewer.from(authentication));
    }
}