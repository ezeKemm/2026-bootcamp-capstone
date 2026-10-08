package com.northstar.crm.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
        assertEquals(CustomerStatus.ACTIVE, customer.getStatus()); // activate once to set to active

        DomainException exception = assertThrows(DomainException.class, customer::activate);
        assertEquals("Only prospects can be activated.", exception.getMessage());
    }
}
