package com.northstar.crm.viewCustomer;

import com.northstar.crm.domain.Customer;
import com.northstar.crm.domain.CustomerId;
import com.northstar.crm.domain.CustomerNotFoundException;
import com.northstar.crm.platform.logging.LogContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.northstar.crm.viewCustomer.dto.CustomerView;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
class ViewCustomerService {

    private static final Logger log = LoggerFactory.getLogger(ViewCustomerService.class);

    private final ViewCustomerRepository repo;

    ViewCustomerService(ViewCustomerRepository repo) {
        this.repo = repo;
    }

    /** Returns the customer, or throws CustomerNotFoundException (-> 404 via GlobalExceptionHandler). */
    @Transactional(readOnly = true)
    CustomerView get(UUID customerId) {
        LogContext.customerOperation(customerId, "view");
        CustomerId id = new CustomerId(customerId);
        Customer customer = repo.findById(id).orElseThrow(() -> new CustomerNotFoundException(id));
        log.info("viewed customer id={} status={}", customerId, customer.getStatus());
        return new CustomerView(
            customer.getCustomerId().value(),
            customer.getFullName(),
            customer.getEmail(),
            customer.getStatus());
    }
}
