package com.northstar.crm.activateCustomer;

import com.northstar.crm.activateCustomer.dto.ActivatedCustomer;
import com.northstar.crm.domain.Customer;
import com.northstar.crm.domain.CustomerId;
import com.northstar.crm.domain.CustomerNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
class ActivateCustomerService {

    private final ActivateCustomerRepository repo;

    ActivateCustomerService(ActivateCustomerRepository repo) {
        this.repo = repo;
    }

    /**
     * Moves a PROSPECT to ACTIVE.
     * Unknown id -> CustomerNotFoundException (404); not a prospect -> DomainException from
     * Customer.activate() (422), both mapped by GlobalExceptionHandler.
     */
    @Transactional
    ActivatedCustomer activate(UUID customerId) {
        CustomerId id = new CustomerId(customerId);
        Customer customer = repo.findById(id).orElseThrow(() -> new CustomerNotFoundException(id));
        customer.activate();
        Customer saved = repo.save(customer);
        return new ActivatedCustomer(
            saved.getCustomerId().value(),
            saved.getFullName(),
            saved.getEmail(),
            saved.getStatus());
    }
}
