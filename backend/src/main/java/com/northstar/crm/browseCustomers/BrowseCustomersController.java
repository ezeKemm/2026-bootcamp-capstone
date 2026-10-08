package com.northstar.crm.browseCustomers;

import com.northstar.crm.browseCustomers.dto.CustomerRecordPage;
import com.northstar.crm.domain.CustomerStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/customers")
class BrowseCustomersController {

    static final int MAX_PAGE_SIZE = 100;

    private final BrowseCustomersService service;

    BrowseCustomersController(BrowseCustomersService service) {
        this.service = service;
    }

    @GetMapping
    public CustomerRecordPage list(
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "20") @Min(1) @Max(MAX_PAGE_SIZE) int size,
        @RequestParam(required = false) CustomerStatus status ) {
        return service.list(page, size, status);
    }
}
