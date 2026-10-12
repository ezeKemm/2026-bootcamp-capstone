package com.northstar.crm.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/** Aggregates are equal by id, not by field values. */
class AggregateEqualityTest {

    private static Customer activeCustomer() {
        Customer customer = Customer.registerProspect("Amina Khan", "amina.khan@example.com");
        customer.activate();
        return customer;
    }

    @Test
    void customersAreEqualByIdNotByFields() {
        Customer customer = activeCustomer();
        assertEquals(customer, customer);
        assertEquals(customer.hashCode(), customer.hashCode());
        assertNotEquals(
            Customer.registerProspect("Amina Khan", "amina.khan@example.com"),
            Customer.registerProspect("Amina Khan", "amina.khan@example.com"));
        assertNotEquals(null, customer);
        assertNotEquals("not a customer", customer);
    }

    @Test
    void interactionsAreEqualByIdNotByContent() {
        Customer customer = activeCustomer();
        CustomerInteraction first = customer.recordInteraction(InteractionChannel.PHONE, "Called.", "agent1", "corr-1");
        CustomerInteraction second = customer.recordInteraction(InteractionChannel.PHONE, "Called.", "agent1", "corr-1");
        assertEquals(first, first);
        assertEquals(first.hashCode(), first.hashCode());
        assertNotEquals(first, second);
        assertNotEquals(null, first);
        assertNotEquals("not an interaction", first);
    }
}
