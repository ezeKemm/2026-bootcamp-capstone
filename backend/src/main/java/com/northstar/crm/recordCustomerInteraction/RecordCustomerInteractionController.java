package com.northstar.crm.recordCustomerInteraction;

import com.northstar.crm.domain.CustomerId;
import com.northstar.crm.platform.logging.CorrelationFilter;
import com.northstar.crm.recordCustomerInteraction.dto.RecordInteractionRequest;
import com.northstar.crm.recordCustomerInteraction.dto.RecordInteractionResponse;
import jakarta.validation.Valid;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/customers")
public class RecordCustomerInteractionController {

    private final RecordCustomerInteractionService service;

    RecordCustomerInteractionController(RecordCustomerInteractionService service) {
        this.service = service;
    }

    @PostMapping("/{customerId}/interactions")
    @ResponseStatus(HttpStatus.CREATED)
    RecordInteractionResponse record(
        @PathVariable UUID customerId,
        @Valid @RequestBody RecordInteractionRequest request,
        Authentication authentication) {

        return service.record(new CustomerId(customerId), request,
                              authentication.getName(), MDC.get(CorrelationFilter.MDC_KEY));
    }
}
