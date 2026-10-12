package com.northstar.crm.recordCustomerInteraction;

import com.northstar.crm.domain.Customer;
import com.northstar.crm.domain.CustomerId;
import com.northstar.crm.domain.CustomerInteraction;
import com.northstar.crm.domain.CustomerNotFoundException;
import com.northstar.crm.platform.logging.LogContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.northstar.crm.recordCustomerInteraction.dto.RecordInteractionRequest;
import com.northstar.crm.recordCustomerInteraction.dto.RecordInteractionResponse;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RecordCustomerInteractionService {
    private static final Logger log = LoggerFactory.getLogger(RecordCustomerInteractionService.class);

    private final InteractionRepository interactionRepository;
    private final CustomerRepository customerRepository;
    private final ApplicationEventPublisher events;

    public RecordCustomerInteractionService(InteractionRepository interaction, CustomerRepository customer, ApplicationEventPublisher events) {
        this.interactionRepository = interaction;
        this.customerRepository = customer;
        this.events = events;
    }

    @Transactional
    RecordInteractionResponse record(
        CustomerId customerId, RecordInteractionRequest request, String actor, String correlationId) {

        LogContext.customerOperation(customerId.value(), "record_interaction");
        Customer customer = customerRepository.findById(customerId)
            .orElseThrow(() -> new CustomerNotFoundException(customerId));

        CustomerInteraction interaction = customer.recordInteraction(request.channel(), request.summary(), actor, correlationId);
        CustomerInteraction saved = interactionRepository.save(interaction);
        // IDs and channel only: the summary is note text and never goes in the logs.
        log.info("recorded interaction id={} customer id={} channel={}",
            saved.getInteractionId().value(), customerId.value(), saved.getChannel());

        events.publishEvent(new InteractionRecorded(saved));

        return RecordInteractionResponse.from(saved);
    }
}
