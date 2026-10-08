package com.northstar.crm.browseCustomers;

import com.northstar.crm.domain.Customer;
import com.northstar.crm.domain.CustomerId;
import com.northstar.crm.domain.CustomerStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.Collection;

interface BrowseCustomersRepository extends Repository<Customer, CustomerId> {

    /**
     * Retrieves a page of customer record for the Browse Customers endpoint.
     * @param pageable The pagination information.
     * @return A page of customer record summaries.
     */
    @Query(value = """
        select c from Customer
        where c.status in :statuses
        order by c.fullName, c.customerId.value
        """,
        countQuery = """
        select count(c) from Customer c
        where c.status in :statuses
        """)
    Page<Customer> findRecords(@Param("statuses") Collection<CustomerStatus> statuses,
                               Pageable pageable);
}
