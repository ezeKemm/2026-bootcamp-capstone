package com.northstar.crm.platform.messaging;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class InteractionEventConsumerTest {

    @Mock
    InteractionEventHandler handler;

    @Test
    void anEmptyEventIsSkippedWithoutCallingTheHandler() {
        var record = new ConsumerRecord<String, CustomerInteractionRecordedV1>("interactions", 0, 8L, "key", null);

        new InteractionEventConsumer(handler).consume(record);

        verifyNoInteractions(handler);
    }
}
