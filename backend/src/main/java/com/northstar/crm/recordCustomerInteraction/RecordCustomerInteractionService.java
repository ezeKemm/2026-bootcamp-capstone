package com.northstar.crm.recordCustomerInteraction;

import com.northstar.crm.domain.Customer;
import com.northstar.crm.domain.CustomerId;
import com.northstar.crm.domain.CustomerInteraction;
import com.northstar.crm.recordCustomerInteraction.dto.RecordInteractionRequest;
import com.northstar.crm.recordCustomerInteraction.dto.RecordInteractionResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RecordCustomerInteractionService {
    private final InteractionRepository interactionRepository;
    private final CustomerRepository customerRepository;

    public RecordCustomerInteractionService(InteractionRepository interaction, CustomerRepository customer) {
        this.interactionRepository = interaction;
        this.customerRepository = customer;
    }

    @Transactional
    RecordInteractionResponse record(
        CustomerId customerId, RecordInteractionRequest request, String actor, String correlationId) {

        // TODO: create Custom Exception to handle this
        Customer customer = customerRepository.findById(customerId)
            .orElseThrow(() -> new IllegalArgumentException("Customer Not Found"));

        CustomerInteraction interaction = customer.recordInteraction(request.channel(), request.summary(), actor, correlationId);

        // TODO: fire event after transaction commits
        return RecordInteractionResponse.from(interactionRepository.save(interaction));
    }
}
