package com.northstar.funding.rest.controller;

import com.northstar.funding.domain.*;
import com.northstar.funding.kafka.events.SearchRequestEvent;
import com.northstar.funding.rest.dto.SearchExecutionRequest;
import com.northstar.funding.rest.dto.SearchExecutionResponse;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.test.mock.mockito.MockBean;
import com.northstar.funding.querygeneration.service.QueryGenerationService;
import com.northstar.funding.persistence.service.DiscoverySessionService;
import com.northstar.funding.querygeneration.model.QueryGenerationResponse;

import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Integration test for SearchController REST API.
 *
 * <p>Tests the complete flow:
 * <ul>
 *   <li>HTTP POST /api/search/execute</li>
 *   <li>Query generation</li>
 *   <li>Kafka event publication to search-requests topic</li>
 * </ul>
 *
 * <p>Uses embedded Kafka broker for testing event publication.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EmbeddedKafka(partitions = 1, topics = {"search-requests"})
@ActiveProfiles("test")
class SearchControllerTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafka;

    @MockBean
    private QueryGenerationService queryGenerationService;

    @MockBean
    private DiscoverySessionService discoverySessionService;

    @Test
    void executeSearch_WithValidRequest_ReturnsInitiatedStatus() {
        // Given - mock query generation service to return 3 queries per engine (9 total)
        when(queryGenerationService.generateQueries(any()))
                .thenReturn(CompletableFuture.completedFuture(
                        QueryGenerationResponse.builder()
                                .queries(List.of("query1", "query2", "query3"))
                                .build()
                ));

        // Given - valid search request
        SearchExecutionRequest request = new SearchExecutionRequest(
                Set.of(FundingSourceType.GOVERNMENT_EU, FundingSourceType.GOVERNMENT_NATIONAL),
                Set.of(FundingMechanism.GRANT, FundingMechanism.SCHOLARSHIP),
                ProjectScale.SMALL,
                Set.of(BeneficiaryPopulation.AT_RISK_YOUTH, BeneficiaryPopulation.EDUCATORS_TEACHERS),
                Set.of(RecipientOrganizationType.K12_PUBLIC_SCHOOL, RecipientOrganizationType.UNIVERSITY_PUBLIC),
                Set.of("Bulgaria", "Eastern Europe"),
                QueryLanguage.ENGLISH,
                25
        );

        // When - POST /api/search/execute
        ResponseEntity<SearchExecutionResponse> response = restTemplate.postForEntity(
                "/api/search/execute",
                request,
                SearchExecutionResponse.class
        );

        // Then - returns 200 OK with session info
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().sessionId()).isNotNull();
        assertThat(response.getBody().queriesGenerated()).isGreaterThan(0);
        assertThat(response.getBody().status()).isEqualTo("INITIATED");
        assertThat(response.getBody().message()).contains("initiated");
    }

    @Test
    void executeSearch_PublishesSearchRequestEventsToKafka() {
        // Given - Kafka consumer with proper deserializer for SearchRequestEvent
        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps(
                "kafka-test-group-" + System.currentTimeMillis(),  // Unique group per test
                "true",
                embeddedKafka
        );
        consumerProps.put(org.apache.kafka.clients.consumer.ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                org.springframework.kafka.support.serializer.JsonDeserializer.class);
        consumerProps.put(org.springframework.kafka.support.serializer.JsonDeserializer.TRUSTED_PACKAGES, "*");
        consumerProps.put(org.springframework.kafka.support.serializer.JsonDeserializer.VALUE_DEFAULT_TYPE,
                SearchRequestEvent.class.getName());

        var consumerFactory = new DefaultKafkaConsumerFactory<String, SearchRequestEvent>(consumerProps);
        var consumer = consumerFactory.createConsumer();
        embeddedKafka.consumeFromAnEmbeddedTopic(consumer, "search-requests");

        // Clear any existing messages by consuming them
        KafkaTestUtils.getRecords(consumer, Duration.ofMillis(100));

        // Given - mock query generation service
        when(queryGenerationService.generateQueries(any()))
                .thenReturn(CompletableFuture.completedFuture(
                        QueryGenerationResponse.builder()
                                .queries(List.of("test query for Kafka"))
                                .build()
                ));

        // Given - valid search request
        SearchExecutionRequest request = new SearchExecutionRequest(
                Set.of(FundingSourceType.GOVERNMENT_EU),
                Set.of(FundingMechanism.GRANT),
                ProjectScale.MEDIUM,
                Set.of(BeneficiaryPopulation.EDUCATORS_TEACHERS),
                Set.of(RecipientOrganizationType.UNIVERSITY_PUBLIC),
                Set.of("Bulgaria"),
                QueryLanguage.ENGLISH,
                20
        );

        // When - POST /api/search/execute
        ResponseEntity<SearchExecutionResponse> response = restTemplate.postForEntity(
                "/api/search/execute",
                request,
                SearchExecutionResponse.class
        );

        // Then - SearchRequestEvents published to Kafka
        // Controller generates queries for 3 engines, so we expect at least 3 events with our sessionId
        var records = KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(5));

        // Filter to only our session's events
        var ourSessionRecords = new java.util.ArrayList<ConsumerRecord<String, SearchRequestEvent>>();
        records.forEach(record -> {
            if (record.value().getSessionId().equals(response.getBody().sessionId())) {
                ourSessionRecords.add(record);
            }
        });

        // Should have exactly 3 events for our session (3 engines × 1 query each)
        assertThat(ourSessionRecords).hasSize(3);

        // Verify all our events have correct structure
        ourSessionRecords.forEach(record -> {
            assertThat(record.value()).isNotNull();
            assertThat(record.value().getSessionId()).isEqualTo(response.getBody().sessionId());
            assertThat(record.value().getQuery()).isEqualTo("test query for Kafka");
            assertThat(record.value().getMaxResults()).isEqualTo(20);
        });

        consumer.close();
    }

    @Test
    void executeSearch_WithMissingRequiredFields_Returns400BadRequest() {
        // Given - invalid request (missing funding source types)
        SearchExecutionRequest request = new SearchExecutionRequest(
                Set.of(), // EMPTY - should fail validation
                Set.of(FundingMechanism.GRANT),
                ProjectScale.SMALL,
                Set.of(BeneficiaryPopulation.EDUCATORS_TEACHERS),
                Set.of(RecipientOrganizationType.UNIVERSITY_PUBLIC),
                Set.of("Bulgaria"),
                QueryLanguage.ENGLISH,
                25
        );

        // When - POST /api/search/execute
        ResponseEntity<SearchExecutionResponse> response = restTemplate.postForEntity(
                "/api/search/execute",
                request,
                SearchExecutionResponse.class
        );

        // Then - returns 400 Bad Request
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void executeSearch_WithInvalidMaxResults_Returns400BadRequest() {
        // Given - invalid max results (too low)
        SearchExecutionRequest request = new SearchExecutionRequest(
                Set.of(FundingSourceType.GOVERNMENT_EU),
                Set.of(FundingMechanism.GRANT),
                ProjectScale.SMALL,
                Set.of(BeneficiaryPopulation.EDUCATORS_TEACHERS),
                Set.of(RecipientOrganizationType.UNIVERSITY_PUBLIC),
                Set.of("Bulgaria"),
                QueryLanguage.ENGLISH,
                5 // TOO LOW - min is 10
        );

        // When - POST /api/search/execute
        ResponseEntity<SearchExecutionResponse> response = restTemplate.postForEntity(
                "/api/search/execute",
                request,
                SearchExecutionResponse.class
        );

        // Then - returns 400 Bad Request
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
