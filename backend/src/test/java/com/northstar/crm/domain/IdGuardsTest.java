package com.northstar.crm.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

/** Identifiers can never hold a null UUID. */
class IdGuardsTest {

    @Test
    void customerIdRejectsNull() {
        assertThrows(NullPointerException.class, () -> new CustomerId(null));
    }

    @Test
    void interactionIdRejectsNull() {
        assertThrows(NullPointerException.class, () -> new InteractionId(null));
    }
}
