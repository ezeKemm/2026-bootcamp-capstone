package com.northstar.crm.recordCustomerInteraction;

import com.northstar.crm.domain.CustomerInteraction;
import com.northstar.crm.domain.InteractionId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

interface InteractionRepository extends JpaRepository<CustomerInteraction, InteractionId> {
    @Override
    CustomerInteraction save(CustomerInteraction entity);

    @Override
    Optional<CustomerInteraction> findById(InteractionId interactionId);
}
