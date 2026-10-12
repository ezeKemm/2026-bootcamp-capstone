package com.northstar.crm.activateCustomer;

import com.northstar.crm.domain.Customer;
import com.northstar.crm.domain.CustomerId;
import org.springframework.data.repository.Repository;

import java.util.Optional;

/** Loads and saves a customer for the Activate Customer slice. */
interface ActivateCustomerRepository extends Repository<Customer, CustomerId> {
    Optional<Customer> findById(CustomerId customerId);

    Customer save(Customer customer);
}
