package com.northstar.crm.activateCustomer;

import com.northstar.crm.activateCustomer.dto.ActivatedCustomer;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** POST /api/v1/customers/{customerId}/activate: AGENT or ADMIN (rule lives in SecurityConfig). No request body. */
@RestController
@RequestMapping("/api/v1/customers")
class ActivateCustomerController {

    private final ActivateCustomerService service;

    ActivateCustomerController(ActivateCustomerService service) {
        this.service = service;
    }

    @PostMapping("/{customerId}/activate")
    ActivatedCustomer activate(@PathVariable UUID customerId) {
        return service.activate(customerId);
    }
}
