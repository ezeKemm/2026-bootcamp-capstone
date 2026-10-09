package com.northstar.crm.recordCustomerInteraction;

import com.northstar.crm.domain.*;
import com.northstar.crm.recordCustomerInteraction.dto.RecordInteractionRequest;
import com.northstar.crm.recordCustomerInteraction.dto.RecordInteractionResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class RecordCustomerInteractionServiceTest {
    @Mock private InteractionRepository interactionRepository;

    @Mock private CustomerRepository customerRepository;

    @Mock private ApplicationEventPublisher events;

    @Test
    void recordsInteractionAndMapsToResponse() {
        Customer customer = Customer.registerProspect("Amina Khan", "amina.khan@example.com");
        CustomerId customerId = customer.getCustomerId();
        customer.activate();

        CustomerInteraction savedInteraction = customer.recordInteraction(
            InteractionChannel.EMAIL,
            "Sent onboarding email.",
            "agent1",
            "lab-request-001"
        );

        when(customerRepository.findById(customer.getCustomerId())).thenReturn(Optional.of(customer));
        when(interactionRepository.save(any(CustomerInteraction.class))).thenReturn(savedInteraction);

        RecordCustomerInteractionService service = new RecordCustomerInteractionService(interactionRepository, customerRepository, events);
        RecordInteractionRequest request = new RecordInteractionRequest(InteractionChannel.EMAIL, "Sent onboarding email.");

        RecordInteractionResponse response = service.record(customerId, request, "agent1", "lab-request-001");

        assertThat(response.customerId()).isEqualTo(customerId.value());
        assertThat(response.actor()).isEqualTo("agent1");
        assertThat(response.channel()).isEqualTo(InteractionChannel.EMAIL);
        assertThat(response.summary()).isEqualTo("Sent onboarding email.");
        assertThat(response.correlationId()).isEqualTo("lab-request-001");
        assertThat(response.interactionId()).isNotNull();
        assertThat(response.occurredAt()).isNotNull();

        verify(interactionRepository).save(any(CustomerInteraction.class));
    }

    @Test
    void throwsWhenCustomerDoesNotExist() {
        CustomerId customerId = CustomerId.generate();
        when(customerRepository.findById(customerId)).thenReturn(Optional.empty());

        RecordCustomerInteractionService service = new RecordCustomerInteractionService(interactionRepository, customerRepository, events);
        RecordInteractionRequest request = new RecordInteractionRequest(InteractionChannel.PHONE, "Called about the account.");

        assertThrows(
            CustomerNotFoundException.class,
            () -> service.record(customerId, request, "agent1", "lab-request-001")
        );

        verifyNoInteractions(interactionRepository);
    }

    @Test
    void propagatesDomainExceptionWhenCustomerIsNotActive() {
        CustomerId customerId = CustomerId.generate();
        Customer prospect = Customer.registerProspect("Ravi Singh", "ravi.singh@example.com");
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(prospect));

        RecordCustomerInteractionService service = new RecordCustomerInteractionService(interactionRepository, customerRepository, events);
        RecordInteractionRequest request = new RecordInteractionRequest(InteractionChannel.CHAT, "Introduced services.");

        DomainException exception = assertThrows(
            DomainException.class,
            () -> service.record(customerId, request, "agent1", "lab-request-001")
        );

        assertThat(exception.getMessage()).isEqualTo("Only active customers can interact with bank staff.");
        verifyNoInteractions(interactionRepository);
    }
}
