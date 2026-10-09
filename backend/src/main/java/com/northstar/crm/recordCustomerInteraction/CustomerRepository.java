package com.northstar.crm.recordCustomerInteraction;

import com.northstar.crm.domain.Customer;
import com.northstar.crm.domain.CustomerId;
import org.springframework.data.repository.Repository;

import java.util.Optional;

interface CustomerRepository extends Repository<Customer, CustomerId> {
    Optional<Customer> findById(CustomerId customerId);
}
