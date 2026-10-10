package com.northstar.crm.recordCustomerInteraction;

import com.northstar.crm.domain.*;
import com.northstar.crm.platform.messaging.CustomerInteractionRecordedV1;
import com.northstar.crm.platform.messaging.InteractionEventPublisher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InteractionRecordedListenerTest {

    @Mock InteractionEventPublisher publisher;

    private CustomerInteraction interaction() {
        Customer customer = Customer.registerProspect("Amina Khan", "amina.khan@example.com");
        customer.activate();
        return customer.recordInteraction(InteractionChannel.valueOf("PHONE"), "called", "agent1", "lab-request-001");
    }

    @Test
    void publishesEventWithFieldsFromSavedInteraction() {
        when(publisher.publish(any())).thenReturn(CompletableFuture.completedFuture(null));
        var i = interaction();

        new InteractionRecordedListener(publisher).on(new InteractionRecorded(i));

        var captor = ArgumentCaptor.forClass(CustomerInteractionRecordedV1.class);
        verify(publisher).publish(captor.capture());
        assertThat(captor.getValue().interactionId()).isEqualTo(i.getInteractionId().value());
        assertThat(captor.getValue().customerId()).isEqualTo(i.getCustomerId().value());
        assertThat(captor.getValue().actor()).isEqualTo("agent1");
        assertThat(captor.getValue().correlationId()).isEqualTo("lab-request-001");
    }

    @Test
    void failedPublishIsSwallowed() {
        when(publisher.publish(any()))
            .thenReturn(CompletableFuture.failedFuture(new RuntimeException("broker down")));

        assertThatCode(() -> new InteractionRecordedListener(publisher)
            .on(new InteractionRecorded(interaction()))).doesNotThrowAnyException();
    }
}