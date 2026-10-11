package com.northstar.crm.activateCustomer.dto;

import com.northstar.crm.domain.CustomerStatus;

import java.util.UUID;

/** The customer after activation, as returned by POST /api/v1/customers/{customerId}/activate (Customer schema in openapi.yaml). */
public record ActivatedCustomer(UUID customerId, String fullName, String email, CustomerStatus status) {}
