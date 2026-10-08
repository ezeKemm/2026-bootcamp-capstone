package com.northstar.crm.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;


@Entity
@Table(name = "customers")
public class Customer {
    private static final int MAX_FULL_NAME_LENGTH = 100;
    private static final int MAX_EMAIL_LENGTH = 100;

    @EmbeddedId
    private CustomerId customerId;

    @Column(name = "full_name", nullable = false, length = MAX_FULL_NAME_LENGTH)
    private String fullName;

    @Column(name = "email", nullable = false, length = MAX_EMAIL_LENGTH)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10)
    private CustomerStatus status;

    protected Customer() {} // for JPA

    private Customer(String fullName, String email, CustomerStatus status) {
        this.customerId = CustomerId.generate();
        this.fullName = fullName;
        this.email = email;
        this.status = status;
    }

    /**
     * Factory method to register a new Customer. All new Customers begin with PROSPECT status.
     * @param fullName The full name of the customer.
     * @param email The email address of the customer.
     * @return A new prospect Customer.
     */
    public static Customer registerProspect(String fullName, String email) {
        return new Customer(fullName, email, CustomerStatus.PROSPECT);
    }

    public void activate() {
        if (status != CustomerStatus.PROSPECT) {
            throw new DomainException("Only prospects can be activated.");
        }
        status = CustomerStatus.ACTIVE;
    }

    // Getters
    public CustomerId getCustomerId() { return customerId; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public CustomerStatus getStatus() { return status; }

    @Override
    public boolean equals(Object o) {
        return this == o ||
            (o instanceof Customer other
                && customerId.equals(other.customerId));
    }

    @Override
    public int hashCode() {
        return customerId.hashCode();
    }
}
