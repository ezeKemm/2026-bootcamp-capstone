package com.northstar.crm.viewCustomer.dto;

import com.northstar.crm.domain.CustomerStatus;

import java.util.UUID;

/** One customer's profile, as returned by GET /api/v1/customers/{customerId} (Customer schema in openapi.yaml). */
public record CustomerView(UUID customerId, String fullName, String email, CustomerStatus status) {}
