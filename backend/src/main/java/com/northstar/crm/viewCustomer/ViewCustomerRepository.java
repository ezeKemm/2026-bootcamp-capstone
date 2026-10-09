package com.northstar.crm.viewCustomer;

import com.northstar.crm.domain.Customer;
import com.northstar.crm.domain.CustomerId;
import org.springframework.data.repository.Repository;

import java.util.Optional;

/** Read-only access to one customer, for the View Customer slice. */
interface ViewCustomerRepository extends Repository<Customer, CustomerId> {
    Optional<Customer> findById(CustomerId customerId);
}
