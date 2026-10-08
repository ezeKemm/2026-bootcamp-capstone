package com.northstar.crm.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

// Identity of a {@link CustomerInteraction}. Wraps UUID.
@Embeddable
public record InteractionId(
	@Column(name = "interaction_id", nullable = false, updatable = false)
	UUID value
) implements Serializable {

	// guard prevents a null value being passed
	public InteractionId {
		Objects.requireNonNull(value, "Interaction id cannot be null");
	}

	public static InteractionId generate() {
		return new InteractionId(UUID.randomUUID());
	}

	@Override
	public String toString() {
		return value.toString();
	}
}
