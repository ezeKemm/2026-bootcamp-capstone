package com.northstar.crm.browsePublicCustomers;

import com.northstar.crm.browsePublicCustomers.dto.PublicCustomerSummaryPage;
import com.northstar.crm.domain.CustomerStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/public/customers")
public class BrowsePublicCustomersController {

    static final int MAX_PAGE_SIZE = 100;

    private final BrowsePublicCustomersService service;

    BrowsePublicCustomersController(BrowsePublicCustomersService service) {
        this.service = service;
    }

    @GetMapping
    public PublicCustomerSummaryPage list(
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "20") @Min(1) @Max(MAX_PAGE_SIZE) int size,
        @RequestParam(required = false) CustomerStatus status ) {
        return service.list(page, size, status);
    }
}
