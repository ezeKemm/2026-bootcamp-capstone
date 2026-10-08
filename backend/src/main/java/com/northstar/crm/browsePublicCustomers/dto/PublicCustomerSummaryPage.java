package com.northstar.crm.browsePublicCustomers.dto;

import com.northstar.crm.browsePublicCustomers.PublicCustomerSummary;

import java.util.List;

/**
 * A page of customer record summaries for the unauthenticated Browse Customers page.
 * @param items The list of public customer records on this page.
 * @param page The current page number (0-based).
 * @param totalItems The total number of customer records available.
 * @param totalPages The total number of pages available.
 */
public record PublicCustomerSummaryPage(List<PublicCustomerSummary> items, int page, int size, long totalItems, int totalPages) {}
