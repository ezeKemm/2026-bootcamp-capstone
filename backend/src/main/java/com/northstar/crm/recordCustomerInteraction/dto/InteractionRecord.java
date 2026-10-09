package com.northstar.crm.recordCustomerInteraction.dto;

import com.northstar.crm.domain.InteractionChannel;

import java.time.Instant;
import java.util.UUID;

/**
 * A DTO wrapping a customer interaction record.
 */
public record InteractionRecord(
    UUID interactionId,
    UUID customerId,
    String actor,
    InteractionChannel channel,
    String summary,
    Instant occurredAt,
    String correlationId
) {}
