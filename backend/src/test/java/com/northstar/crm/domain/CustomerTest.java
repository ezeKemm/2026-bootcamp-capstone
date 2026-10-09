package com.northstar.crm.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class CustomerTest {
    @Test
    void activatingProspectiveCustomerSetsActive() {
        Customer customer = Customer.registerProspect("Amina Khan", "amina.khan@example.com");
        assertEquals(CustomerStatus.PROSPECT, customer.getStatus());
        customer.activate();
        assertEquals(CustomerStatus.ACTIVE, customer.getStatus());
    }

    @Test
    void activatingActiveCustomerThrows() {
        Customer customer = Customer.registerProspect("Amina Khan", "amina.khan@example.com");
        customer.activate();
        assertEquals(CustomerStatus.ACTIVE, customer.getStatus());

        DomainException exception = assertThrows(DomainException.class, customer::activate);
        assertEquals("Only prospects can be activated.", exception.getMessage());
    }

    @Test
    void recordingInteractionForActiveCustomerReturnsInteraction() {
        Customer customer = Customer.registerProspect("Amina Khan", "amina.khan@example.com");
        customer.activate();

        CustomerInteraction interaction = customer.recordInteraction(
            InteractionChannel.EMAIL,
            "Sent onboarding email.",
            "agent1",
            "lab-request-001"
        );

        assertNotNull(interaction);
        assertEquals(customer.getCustomerId(), interaction.getCustomerId());
        assertEquals(InteractionChannel.EMAIL, interaction.getChannel());
        assertEquals("Sent onboarding email.", interaction.getSummary());
        assertEquals("agent1", interaction.getActor());
        assertEquals("lab-request-001", interaction.getCorrelationId());
        assertNotNull(interaction.getOccurredAt());
    }

    @Test
    void recordingInteractionForProspectThrows() {
        Customer customer = Customer.registerProspect("Ravi Singh", "ravi.singh@example.com");

        DomainException exception = assertThrows(
            DomainException.class,
            () -> customer.recordInteraction(
                InteractionChannel.PHONE,
                "Called about account setup.",
                "agent1",
                "lab-request-001"
            )
        );

        assertEquals("Only active customers can interact with bank staff.", exception.getMessage());
    }
}
