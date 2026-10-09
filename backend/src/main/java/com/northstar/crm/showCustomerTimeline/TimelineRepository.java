package com.northstar.crm.showCustomerTimeline;

import com.northstar.crm.domain.CustomerInteraction;
import com.northstar.crm.domain.InteractionId;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

interface ShowCustomerTimelineRepository extends Repository<CustomerInteraction, InteractionId> {

    @Query("select count(c) > 0 from Customer c where c.customerId.value = :customerId")
    boolean customerExists(@Param("customerId") UUID customerId);

    /** ADMIN path: every interaction for the customer, newest first. */
    @Query("""
        select i from CustomerInteraction i
        where i.customerId.value = :customerId
        order by i.occurredAt desc, i.interactionId.value
        """)
    List<CustomerInteraction> findTimeline(@Param("customerId") UUID customerId);

    /** AGENT path: only interactions this actor recorded, newest first. */
    @Query("""
        select i from CustomerInteraction i
        where i.customerId.value = :customerId
        and i.actor = :actor
        order by i.occurredAt desc, i.interactionId.value
        """)
    List<CustomerInteraction> findTimelineByActor(@Param("customerId") UUID customerId,
                                                @Param("actor") String actor);
}