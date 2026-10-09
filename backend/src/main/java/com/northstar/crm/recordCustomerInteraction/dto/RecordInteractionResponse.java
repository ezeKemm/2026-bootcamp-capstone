package com.northstar.crm.recordCustomerInteraction.dto;

import com.northstar.crm.domain.CustomerInteraction;
import com.northstar.crm.domain.InteractionChannel;

import java.time.Instant;
import java.util.UUID;

/** Response body for recorded customer interactions. */
public record RecordInteractionResponse(
    UUID interactionId,
    UUID customerId,
    String actor,
    InteractionChannel channel,
    String summary,
    Instant occurredAt,
    String correlationId
) {
    /** Map a CustomerInteraction domain object to a RecordInteractionResponse DTO. */
    public static RecordInteractionResponse from(CustomerInteraction interaction) {
        return new RecordInteractionResponse(
            interaction.getInteractionId().value(),
            interaction.getCustomerId().value(),
            interaction.getActor(),
            interaction.getChannel(),
            interaction.getSummary(),
            interaction.getOccurredAt(),
            interaction.getCorrelationId()
        );
    }
}
