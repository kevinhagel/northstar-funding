package com.northstar.funding.rest.util;

import com.northstar.funding.kafka.events.SearchRequestEvent;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Utility class for asserting expected Kafka event state in integration tests.
 * Provides fluent assertions for Kafka message verification scenarios.
 */
public class ExpectedKafkaEvents {

    /**
     * Assert that Kafka events were published with correct structure and sessionId.
     *
     * @param events         List of consumed events
     * @param expectedSessionId Expected session ID in all events
     * @param expectedCount  Expected number of events
     */
    public static void assertEventsPublished(
        List<SearchRequestEvent> events,
        UUID expectedSessionId,
        int expectedCount
    ) {
        // Verify event count
        assertThat(events)
            .as("Should have exactly %d events published", expectedCount)
            .hasSize(expectedCount);

        // Verify all events have correct sessionId
        events.forEach(event -> {
            assertThat(event.getSessionId())
                .as("Event sessionId should match expected %s", expectedSessionId)
                .isEqualTo(expectedSessionId);

            assertThat(event.getQuery())
                .as("Event query should not be blank")
                .isNotBlank();

            assertThat(event.getMaxResults())
                .as("Event maxResults should be >= 10")
                .isGreaterThanOrEqualTo(10);
        });
    }

    /**
     * Assert that NO Kafka events were published.
     * Useful for verifying that invalid requests don't publish messages.
     *
     * @param events List of consumed events (should be empty)
     */
    public static void assertNoEventsPublished(List<SearchRequestEvent> events) {
        assertThat(events)
            .as("No events should be published after invalid request")
            .isEmpty();
    }

    /**
     * Consume Kafka events from the search-requests topic and filter by sessionId.
     * Polls Kafka for up to 5 seconds to allow messages to arrive.
     *
     * @param sessionId    Session ID to filter events
     * @param consumer     Kafka consumer
     * @param pollDuration How long to poll for messages
     * @return List of events matching the sessionId
     */
    public static List<SearchRequestEvent> consumeKafkaEvents(
        UUID sessionId,
        Consumer<String, SearchRequestEvent> consumer,
        Duration pollDuration
    ) {
        consumer.subscribe(Collections.singleton("search-requests"));

        // Poll Kafka (may need multiple polls for all messages)
        ConsumerRecords<String, SearchRequestEvent> records = consumer.poll(pollDuration);

        // Filter events by sessionId
        return StreamSupport.stream(records.spliterator(), false)
            .map(record -> record.value())
            .filter(event -> event.getSessionId().equals(sessionId))
            .toList();
    }

    /**
     * Create a Kafka consumer for integration tests.
     * Configures consumer to read from earliest offset with JSON deserialization.
     *
     * @param bootstrapServers Kafka bootstrap servers from TestContainers
     * @return Configured Kafka consumer
     */
    public static org.apache.kafka.clients.consumer.KafkaConsumer<String, SearchRequestEvent> createTestConsumer(
        String bootstrapServers
    ) {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "integration-test-consumer-" + UUID.randomUUID());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, SearchRequestEvent.class.getName());

        return new org.apache.kafka.clients.consumer.KafkaConsumer<>(props);
    }
}
