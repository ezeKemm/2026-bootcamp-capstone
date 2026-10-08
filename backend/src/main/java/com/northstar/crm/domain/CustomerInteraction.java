package com.northstar.crm.domain;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Embedded;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;

import java.time.Instant;

@Entity
@Table(name = "customer_interactions")
public class CustomerInteraction {
    public static final int SUMMARY_MAX_LENGTH = 500;
    public static final int ACTOR_MAX_LENGTH = 30;
    public static final int CORRELATION_ID_MAX_LENGTH = 60;

    @EmbeddedId
    private InteractionId interactionId;

    @Embedded
    private CustomerId customerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, updatable = false, length = 10)
    private InteractionChannel channel;

    @Column(name = "summary", nullable = false, updatable = false, length = SUMMARY_MAX_LENGTH)
    private String summary;

    @Column(name = "actor", nullable = false, updatable = false, length = ACTOR_MAX_LENGTH)
    private String actor;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    @Column(name = "correlation_id", nullable = false, updatable = false, length = CORRELATION_ID_MAX_LENGTH)
    private String correlationId;

    protected CustomerInteraction() {} // for JPA

    // Package-private: Interactions must be created through Customer aggregate
    CustomerInteraction( CustomerId customerId, InteractionChannel channel, String summary,
                                String actor, Instant occurredAt, String correlationId) {
        this.interactionId = InteractionId.generate();
        this.customerId = customerId;
        this.channel = channel;
        this.summary = summary;
        this.actor = actor;
        this.occurredAt = occurredAt;
        this.correlationId = correlationId;
    }

    public InteractionId getInteractionId() { return interactionId; }
    public CustomerId getCustomerId() { return customerId; }
    public InteractionChannel getChannel() { return channel; }
    public String getSummary() { return summary; }
    public String getActor() { return actor; }
    public Instant getOccurredAt() { return occurredAt; }
    public String getCorrelationId() { return correlationId; }

    @Override
    public boolean equals(Object o) {
        return this == o ||
            (o instanceof CustomerInteraction other
                && interactionId.equals(other.interactionId));
    }

    @Override
    public int hashCode() {
        return interactionId.hashCode();
    }
}
