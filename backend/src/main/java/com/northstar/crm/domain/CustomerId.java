package com.northstar.crm.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

// Identity of a {@link Customer}. Wraps UUID.
@Embeddable
public record CustomerId(
    // nullable = false is a schema hint but won't validate plain Java
    @Column(name = "customer_id", nullable = false, updatable = false)
    UUID value
) implements Serializable {
    // guard prevents a null value being passed
    public CustomerId {
        Objects.requireNonNull(value, "Customer id cannot be null");
    }

    public static CustomerId generate() {
        return new CustomerId(UUID.randomUUID());
    }

    public static CustomerId of(String id) {
        return new CustomerId(UUID.fromString(id));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
