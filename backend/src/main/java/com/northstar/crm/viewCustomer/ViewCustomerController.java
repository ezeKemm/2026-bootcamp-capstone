package com.northstar.crm.viewCustomer;

import com.northstar.crm.viewCustomer.dto.CustomerView;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** GET /api/v1/customers/{customerId}: AGENT or ADMIN (rule lives in SecurityConfig). */
@RestController
@RequestMapping("/api/v1/customers")
class ViewCustomerController {

    private final ViewCustomerService service;

    ViewCustomerController(ViewCustomerService service) {
        this.service = service;
    }

    @GetMapping("/{customerId}")
    CustomerView get(@PathVariable UUID customerId) {
        return service.get(customerId);
    }
}
