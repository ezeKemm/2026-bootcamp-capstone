package com.northstar.crm.platform.messaging;

import jakarta.annotation.PreDestroy;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.ListenerExecutionFailedException;
import org.springframework.kafka.support.serializer.DelegatingByTypeSerializer;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

@Configuration(proxyBeanMethods = false)
public class KafkaConsumerErrorConfiguration {

    private static final Logger log =
            LoggerFactory.getLogger(KafkaConsumerErrorConfiguration.class);

    private DefaultKafkaProducerFactory<String, Object> deadLetterProducerFactory;

    @Bean
    public DefaultErrorHandler interactionConsumerErrorHandler(
            ProducerFactory<String, CustomerInteractionRecordedV1> producerFactory,
            @Value("${crm.messaging.dead-letter-topic}") String deadLetterTopic,
            @Value("${crm.messaging.retry-interval-ms}") long retryIntervalMs
    ) {
        Map<Class<?>, Serializer<?>> serializers = Map.of(
                byte[].class, new ByteArraySerializer(),
                CustomerInteractionRecordedV1.class, new JacksonJsonSerializer<>());

        // Deserialization failures must retain the original bytes in the DLT.
        // Keep this producer separate from the application's event publisher.
        deadLetterProducerFactory = new DefaultKafkaProducerFactory<>(
                new HashMap<>(producerFactory.getConfigurationProperties()),
                new StringSerializer(), new DelegatingByTypeSerializer(serializers));
        var deadLetterTemplate = new KafkaTemplate<>(deadLetterProducerFactory);

        var recoverer = new DeadLetterPublishingRecoverer(deadLetterTemplate,
                (record, exception) -> new TopicPartition(deadLetterTopic, record.partition()));
        recoverer.setFailIfSendResultIsError(true);

        // Two retries plus the original delivery = three processing attempts.
        var handler = new DefaultErrorHandler((record, exception) -> {
            recoverer.accept(record, exception);
            Throwable failure = exception;
            while (failure instanceof ListenerExecutionFailedException && failure.getCause() != null) {
                failure = failure.getCause();
            }
            log.warn("Recovered Kafka event to DLT: originalTopic={}, partition={}, offset={}, "
                            + "failureType={}, deadLetterTopic={}",
                    record.topic(), record.partition(), record.offset(),
                    failure.getClass().getSimpleName(), deadLetterTopic);
        }, new FixedBackOff(retryIntervalMs, 2L));
        handler.addNotRetryableExceptions(IllegalArgumentException.class);
        return handler;
    }

    @PreDestroy
    public void closeDeadLetterProducer() {
        if (deadLetterProducerFactory != null) {
            deadLetterProducerFactory.destroy();
        }
    }
}
