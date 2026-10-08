package com.northstar.crm.browsePublicCustomers;

import com.northstar.crm.browsePublicCustomers.dto.PublicCustomerSummaryPage;
import com.northstar.crm.domain.CustomerStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class BrowsePublicCustomersService {

    private final BrowsePublicCustomersRepository repo;

    BrowsePublicCustomersService(BrowsePublicCustomersRepository repo) {
        this.repo = repo;
    }

    /**
     *
     * @param page page number (0-based)
     * @param size page size
     * @param status filter on customer status
     * @return Page DTO wrapping read model {@link PublicCustomerSummary}
     */
    @Transactional(readOnly = true)
    PublicCustomerSummaryPage list(int page, int size, CustomerStatus status) {
        Page<PublicCustomerSummary> result = repo.findRecordSummaries(
            statusesFor(status), PageRequest.of(page, size));
        // Map read model to DTO for API response
        return new PublicCustomerSummaryPage(result.getContent(), result.getNumber(),
            result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    /** An empty filter queries for all statuses by mapping to filter criteria CustomerStatus values */
    static List<CustomerStatus> statusesFor(CustomerStatus status) {
        return status == null ? List.of(CustomerStatus.values()) : List.of(status);
    }
}
