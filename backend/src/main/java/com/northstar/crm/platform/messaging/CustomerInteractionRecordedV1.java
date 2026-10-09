package com.northstar.crm.platform.messaging;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record CustomerInteractionRecordedV1(
        UUID eventId,
        String eventType,
        int eventVersion,
        Instant occurredAt,
        String correlationId,
        String actor,
        UUID customerId,
        UUID interactionId,
        String channel
) {
    public static final String TYPE = "CustomerInteractionRecorded";
    public static final int VERSION = 1;

    public CustomerInteractionRecordedV1 {
        Objects.requireNonNull(eventId, "eventId is required");
        Objects.requireNonNull(occurredAt, "occurredAt is required");
        Objects.requireNonNull(customerId, "customerId is required");
        Objects.requireNonNull(interactionId, "interactionId is required");

        if (!TYPE.equals(eventType)) {
            throw new IllegalArgumentException("Unsupported eventType");
        }

        if (eventVersion != VERSION) {
            throw new IllegalArgumentException("Unsupported eventVersion");
        }

        requireText(correlationId, "correlationId");
        requireText(actor, "actor");
        requireText(channel, "channel");
    }

    public static CustomerInteractionRecordedV1 create(
            UUID eventId,
            Instant occurredAt,
            String correlationId,
            String actor,
            UUID customerId,
            UUID interactionId,
            String channel
    ) {
        return new CustomerInteractionRecordedV1(
                eventId,
                TYPE,
                VERSION,
                occurredAt,
                correlationId,
                actor,
                customerId,
                interactionId,
                channel
        );
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
    }
}