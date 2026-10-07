package com.northstar.crm.domain;

// Throws when a business rule is violated in the application flow
// Maps to 422 Unprocessable Entity at the API edge
public class DomainException extends RuntimeException {
    public DomainException(String message) {
        super(message);
    }
}
