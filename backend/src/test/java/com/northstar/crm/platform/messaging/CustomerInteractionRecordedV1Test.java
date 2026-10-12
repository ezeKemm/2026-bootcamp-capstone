package com.northstar.crm.platform.messaging;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;

/** The event is the contract between producer and consumer; a bad event must fail fast (and go to the DLT). */
class CustomerInteractionRecordedV1Test {

    private static final Instant NOW = Instant.parse("2026-10-08T12:00:00Z");
    private static final UUID ID = UUID.randomUUID();

    @Test
    void missingIdsAndTimestampAreRejected() {
        assertThrows(NullPointerException.class, () -> CustomerInteractionRecordedV1.create(null, NOW, "c", "a", ID, ID, "PHONE"));
        assertThrows(NullPointerException.class, () -> CustomerInteractionRecordedV1.create(ID, null, "c", "a", ID, ID, "PHONE"));
        assertThrows(NullPointerException.class, () -> CustomerInteractionRecordedV1.create(ID, NOW, "c", "a", null, ID, "PHONE"));
        assertThrows(NullPointerException.class, () -> CustomerInteractionRecordedV1.create(ID, NOW, "c", "a", ID, null, "PHONE"));
    }

    @Test
    void unsupportedTypeOrVersionIsRejected() {
        assertThrows(IllegalArgumentException.class, () ->
            new CustomerInteractionRecordedV1(ID, "SomethingElse", 1, NOW, "c", "a", ID, ID, "PHONE"));
        assertThrows(IllegalArgumentException.class, () ->
            new CustomerInteractionRecordedV1(ID, CustomerInteractionRecordedV1.TYPE, 2, NOW, "c", "a", ID, ID, "PHONE"));
    }

    @Test
    void missingTextFieldsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> CustomerInteractionRecordedV1.create(ID, NOW, null, "a", ID, ID, "PHONE"));
        assertThrows(IllegalArgumentException.class, () -> CustomerInteractionRecordedV1.create(ID, NOW, "c", " ", ID, ID, "PHONE"));
        assertThrows(IllegalArgumentException.class, () -> CustomerInteractionRecordedV1.create(ID, NOW, "c", "a", ID, ID, ""));
    }
}
