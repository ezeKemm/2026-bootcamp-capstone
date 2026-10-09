package com.northstar.crm.recordCustomerInteraction.dto;

import com.northstar.crm.domain.CustomerInteraction;
import com.northstar.crm.domain.InteractionChannel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Request body for POST /api/v1/customers/{customerId}/interactions. */
public record RecordInteractionRequest(
    @NotNull(message = "channel is required") InteractionChannel channel,
    @NotBlank(message = "summary is required")
    @Size(max = CustomerInteraction.SUMMARY_MAX_LENGTH, message = "summary must be at most 500 characters")
    String summary
) {}
