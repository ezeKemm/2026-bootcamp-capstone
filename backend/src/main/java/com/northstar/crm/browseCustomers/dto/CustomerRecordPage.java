package com.northstar.crm.browseCustomers.dto;

import com.northstar.crm.browseCustomers.dto.CustomerRecord;

import java.util.List;

/**
 * A page of customer records for the authenticated Browse Customers page.
 * @param items The list of customer records on this page.
 * @param page The current page number (0-based).
 * @param totalItems The total number of customer records available.
 * @param totalPages The total number of pages available.
 */
public record CustomerRecordPage(List<CustomerRecord> items, int page, int size, long totalItems, int totalPages) {}
