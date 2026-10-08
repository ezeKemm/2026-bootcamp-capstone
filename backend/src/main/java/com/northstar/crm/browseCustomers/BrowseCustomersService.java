package com.northstar.crm.browseCustomers;

import com.northstar.crm.browseCustomers.dto.CustomerRecord;
import com.northstar.crm.browseCustomers.dto.CustomerRecordPage;
import com.northstar.crm.domain.Customer;
import com.northstar.crm.domain.CustomerStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
class BrowseCustomersService {

    private final BrowseCustomersRepository repo;

    BrowseCustomersService(BrowseCustomersRepository repo) {
        this.repo = repo;
    }

    /**
     *
     * @param page page number (0-based)
     * @param size page size
     * @param status filter on customer status
     * @return Pageable DTO wrapping domain aggregate {@link Customer}
     */
    @Transactional(readOnly = true)
    CustomerRecordPage list(int page, int size, CustomerStatus status) {
        Page<Customer> result = repo.findRecords(
            statusesFor(status), PageRequest.of(page, size));

        List<CustomerRecord> records = result.getContent().stream()
            .map(BrowseCustomersService::toRecord)
            .toList();

        return new CustomerRecordPage(records, result.getNumber(),
            result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    /** Maps a domain entity to a DTO for API response */
    private static CustomerRecord toRecord(Customer customer) {
        return new CustomerRecord(customer.getCustomerId().value(),
            customer.getFullName(), customer.getEmail(), customer.getStatus());
    }
    /** An empty filter queries for all statuses by mapping to filter criteria CustomerStatus values */
    static List<CustomerStatus> statusesFor(CustomerStatus status) {
        return status == null ? List.of(CustomerStatus.values()) : List.of(status);
    }
}
