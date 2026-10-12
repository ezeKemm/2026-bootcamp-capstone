package com.northstar.crm.recordCustomerInteraction;

import com.northstar.crm.domain.*;
import com.northstar.crm.platform.messaging.CustomerInteractionRecordedV1;
import com.northstar.crm.platform.messaging.Outbox;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class InteractionRecordedListenerTest {

    @Mock Outbox outbox;

    private CustomerInteraction interaction() {
        Customer customer = Customer.registerProspect("Amina Khan", "amina.khan@example.com");
        customer.activate();
        return customer.recordInteraction(InteractionChannel.valueOf("PHONE"), "called", "agent1", "lab-request-001");
    }

    @Test
    void addsOutboxEventFromSavedInteraction() {
        var i = interaction();

        new InteractionRecordedListener(outbox).on(new InteractionRecorded(i));

        var captor = ArgumentCaptor.forClass(CustomerInteractionRecordedV1.class);
        verify(outbox).add(captor.capture());
        assertThat(captor.getValue().interactionId()).isEqualTo(i.getInteractionId().value());
        assertThat(captor.getValue().customerId()).isEqualTo(i.getCustomerId().value());
        assertThat(captor.getValue().actor()).isEqualTo("agent1");
        assertThat(captor.getValue().correlationId()).isEqualTo("lab-request-001");
    }

    @Test
    void outboxFailureRollsBackInteraction() {
        doThrow(new IllegalStateException("database timeout")).when(outbox).add(any());

        assertThatThrownBy(() -> new InteractionRecordedListener(outbox)
            .on(new InteractionRecorded(interaction())))
            .isInstanceOf(IllegalStateException.class);
    }
}