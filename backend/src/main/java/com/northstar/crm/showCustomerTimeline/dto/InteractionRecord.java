package com.northstar.crm.showCustomerTimeline.dto;

import com.northstar.crm.domain.InteractionChannel;

import java.time.Instant;
import java.util.UUID;

/** One timeline entry. Mirrors the Interaction schema in openapi.yaml. */
public record InteractionRecord(
    UUID interactionId,
    UUID customerId,
    InteractionChannel channel,
    String summary,
    String actor,
    Instant occurredAt,
    String correlationId
) {}