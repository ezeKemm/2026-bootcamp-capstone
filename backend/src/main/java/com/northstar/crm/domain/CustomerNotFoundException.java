package com.northstar.crm.domain;

// Thrown when no customer exists with the given id.
// Maps to 404 Not Found at the API edge (see GlobalExceptionHandler).
public class CustomerNotFoundException extends RuntimeException {
    public CustomerNotFoundException(CustomerId customerId) {
        super("No customer with id " + customerId + ".");
    }
}