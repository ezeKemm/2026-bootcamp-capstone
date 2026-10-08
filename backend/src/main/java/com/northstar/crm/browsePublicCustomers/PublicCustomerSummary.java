package com.northstar.crm.browsePublicCustomers;

import com.northstar.crm.domain.CustomerStatus;

/**
 * Public view of a customer record for the unauthenticated Browse Customers endpoint.
 * Represents the read model.
 * @param fullName
 * @param status
 */
public record PublicCustomerSummary(String fullName, CustomerStatus status) {}
