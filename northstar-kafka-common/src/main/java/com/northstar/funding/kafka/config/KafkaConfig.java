package com.northstar.funding.kafka.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.*;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

import static com.northstar.funding.kafka.topics.KafkaTopics.*;

/**
 * Kafka configuration for NorthStar funding discovery workflow.
 *
 * <p>Configures:
 * <ul>
 *   <li>Producer for publishing events to Kafka topics</li>
 *   <li>Consumer for listening to events from Kafka topics</li>
 *   <li>Topic creation with retention policies</li>
 *   <li>JSON serialization/deserialization with Jackson</li>
 * </ul>
 */
@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers:192.168.1.10:9092}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id:northstar-funding-group}")
    private String consumerGroupId;

    /**
     * ObjectMapper for JSON serialization with Java 8 time support.
     */
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }

    /**
     * Kafka producer configuration.
     */
    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        props.put(ProducerConfig.ACKS_CONFIG, "all"); // Wait for all replicas
        props.put(ProducerConfig.RETRIES_CONFIG, 3);
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true); // Prevent duplicates
        return new DefaultKafkaProducerFactory<>(props);
    }

    /**
     * KafkaTemplate for publishing events.
     */
    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    /**
     * Kafka consumer configuration.
     */
    @Bean
    public ConsumerFactory<String, Object> consumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, consumerGroupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest"); // Start from beginning
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "com.northstar.funding.kafka.events");
        props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false); // Disable type headers
        return new DefaultKafkaConsumerFactory<>(props);
    }

    /**
     * Kafka listener container factory for @KafkaListener annotations.
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        factory.setConcurrency(3); // 3 concurrent consumers per topic
        return factory;
    }

    /**
     * Topic: search-requests (7-day retention).
     */
    @Bean
    public NewTopic searchRequestsTopic() {
        return TopicBuilder.name(SEARCH_REQUESTS)
                .partitions(3)
                .replicas(1)
                .config("retention.ms", String.valueOf(7 * 24 * 60 * 60 * 1000L)) // 7 days
                .build();
    }

    /**
     * Topic: search-results-raw (7-day retention).
     */
    @Bean
    public NewTopic searchResultsRawTopic() {
        return TopicBuilder.name(SEARCH_RESULTS_RAW)
                .partitions(3)
                .replicas(1)
                .config("retention.ms", String.valueOf(7 * 24 * 60 * 60 * 1000L)) // 7 days
                .build();
    }

    /**
     * Topic: search-results-validated (7-day retention).
     */
    @Bean
    public NewTopic searchResultsValidatedTopic() {
        return TopicBuilder.name(SEARCH_RESULTS_VALIDATED)
                .partitions(3)
                .replicas(1)
                .config("retention.ms", String.valueOf(7 * 24 * 60 * 60 * 1000L)) // 7 days
                .build();
    }

    /**
     * Topic: workflow-errors (30-day retention for error analysis).
     */
    @Bean
    public NewTopic workflowErrorsTopic() {
        return TopicBuilder.name(WORKFLOW_ERRORS)
                .partitions(1) // Errors are less frequent, single partition
                .replicas(1)
                .config("retention.ms", String.valueOf(30 * 24 * 60 * 60 * 1000L)) // 30 days
                .build();
    }
}
