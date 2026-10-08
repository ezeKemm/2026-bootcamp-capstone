package com.northstar.crm.browseCustomers.dto;

import com.northstar.crm.domain.CustomerStatus;

import java.util.UUID;

/**
 * Public view of a customer record for the unauthenticated Browse Customers endpoint.
 * Represents the read model.
 * @param fullName
 * @param status
 */
public record CustomerRecord(UUID customerId, String fullName, String email, CustomerStatus status) {}
