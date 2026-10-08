package com.northstar.crm.customer;

import com.northstar.crm.domain.Customer;
import com.northstar.crm.domain.CustomerId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository
        extends JpaRepository<Customer, CustomerId> {
}