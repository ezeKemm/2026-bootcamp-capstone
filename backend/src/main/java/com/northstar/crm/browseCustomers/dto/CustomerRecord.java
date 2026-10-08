package com.northstar.crm.browseCustomers.dto;

import com.northstar.crm.domain.CustomerStatus;

import java.util.UUID;

/**
 * View of a customer record for the authenticated Browse Customers endpoint.
 * Represents the read model.
 * @param customerId
 * @param fullName
 * @param email
 * @param status
 */
public record CustomerRecord(UUID customerId, String fullName, String email, CustomerStatus status) {}
