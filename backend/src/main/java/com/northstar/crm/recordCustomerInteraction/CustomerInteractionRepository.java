package com.northstar.crm.recordCustomerInteraction;

import com.northstar.crm.domain.CustomerId;
import com.northstar.crm.domain.CustomerInteraction;
import com.northstar.crm.domain.InteractionId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CustomerInteractionRepository
        extends JpaRepository<CustomerInteraction, InteractionId> {

    List<CustomerInteraction> findByCustomerIdOrderByOccurredAtDesc(
            CustomerId customerId);
}