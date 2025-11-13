package com.northstar.funding.workflow.kafka;

import com.northstar.funding.domain.Domain;
import com.northstar.funding.domain.DomainStatus;
import com.northstar.funding.kafka.events.SearchResultEvent;
import com.northstar.funding.workflow.service.DomainBlacklistCache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Test for SearchResultConsumer (Kafka consumer for search-results-raw topic).
 *
 * <p>Tests result validation, blacklist checking, deduplication,
 * publishing to validated topic, and error handling.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SearchResultConsumerTest {

    @Mock
    private DomainBlacklistCache blacklistCache;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Mock
    private Acknowledgment acknowledgment;

    private SearchResultConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new SearchResultConsumer(blacklistCache, kafkaTemplate);
    }

    @Test
    void consumeSearchResult_whenValidNonBlacklistedDomain_shouldPublishToValidatedTopic() {
        // Given
        UUID sessionId = UUID.randomUUID();
        SearchResultEvent event = SearchResultEvent.builder()
                .sessionId(sessionId)
                .url("https://education.gov.bg/grants")
                .title("Bulgarian Education Grants")
                .description("Apply for education grants in Bulgaria")
                .build();

        when(blacklistCache.isBlacklisted("education.gov.bg")).thenReturn(false);

        // When
        consumer.consumeSearchResult(event, acknowledgment);

        // Then
        verify(blacklistCache).isBlacklisted("education.gov.bg");
        verify(kafkaTemplate).send(eq("search-results-validated"), any());
        verify(acknowledgment).acknowledge();
    }

    @Test
    void consumeSearchResult_whenBlacklistedDomain_shouldNotPublishAndAcknowledge() {
        // Given
        SearchResultEvent event = SearchResultEvent.builder()
                .sessionId(UUID.randomUUID())
                .url("https://spam.xyz/fake-grants")
                .title("Free Money!!!")
                .description("Click here for free grants")
                .build();

        when(blacklistCache.isBlacklisted("spam.xyz")).thenReturn(true);

        // When
        consumer.consumeSearchResult(event, acknowledgment);

        // Then
        verify(blacklistCache).isBlacklisted("spam.xyz");
        verify(kafkaTemplate, never()).send(eq("search-results-validated"), any());
        verify(acknowledgment).acknowledge();
    }

    @Test
    void consumeSearchResult_whenInvalidUrl_shouldLogErrorAndAcknowledge() {
        // Given
        SearchResultEvent event = SearchResultEvent.builder()
                .sessionId(UUID.randomUUID())
                .url("not-a-valid-url")
                .title("Test")
                .description("Test description")
                .build();

        // When
        consumer.consumeSearchResult(event, acknowledgment);

        // Then
        verify(kafkaTemplate, never()).send(anyString(), any());
        verify(acknowledgment).acknowledge();
    }

    @Test
    void consumeSearchResult_whenNullUrl_shouldHandleGracefully() {
        // Given
        SearchResultEvent event = SearchResultEvent.builder()
                .sessionId(UUID.randomUUID())
                .url(null)
                .title("Test")
                .description("Test description")
                .build();

        // When/Then - should not throw exception
        assertThatCode(() -> consumer.consumeSearchResult(event, acknowledgment))
                .doesNotThrowAnyException();
        verify(acknowledgment).acknowledge();
    }

    @Test
    void consumeSearchResult_whenBlacklistCacheThrowsException_shouldPublishErrorAndAcknowledge() {
        // Given
        SearchResultEvent event = SearchResultEvent.builder()
                .sessionId(UUID.randomUUID())
                .url("https://test.com/page")
                .title("Test")
                .description("Test description")
                .build();

        when(blacklistCache.isBlacklisted("test.com"))
                .thenThrow(new RuntimeException("Database connection failed"));

        // When
        consumer.consumeSearchResult(event, acknowledgment);

        // Then
        verify(kafkaTemplate).send(eq("workflow-errors"), any());
        verify(acknowledgment).acknowledge();
    }

    @Test
    void consumeSearchResult_whenNullEvent_shouldHandleGracefully() {
        // When/Then - should not throw exception
        assertThatCode(() -> consumer.consumeSearchResult(null, acknowledgment))
                .doesNotThrowAnyException();
        verify(acknowledgment).acknowledge();
    }

    @Test
    void consumeSearchResult_whenUrlWithPath_shouldExtractDomainCorrectly() {
        // Given
        SearchResultEvent event = SearchResultEvent.builder()
                .sessionId(UUID.randomUUID())
                .url("https://www.example.org/programs/education/grants?year=2024")
                .title("Education Grants")
                .description("Various education grant opportunities")
                .build();

        when(blacklistCache.isBlacklisted("www.example.org")).thenReturn(false);

        // When
        consumer.consumeSearchResult(event, acknowledgment);

        // Then
        verify(blacklistCache).isBlacklisted("www.example.org");
        verify(kafkaTemplate).send(eq("search-results-validated"), any());
        verify(acknowledgment).acknowledge();
    }

    @Test
    void consumeSearchResult_whenSubdomain_shouldExtractFullDomainCorrectly() {
        // Given
        SearchResultEvent event = SearchResultEvent.builder()
                .sessionId(UUID.randomUUID())
                .url("https://funding.research.edu.eu/opportunities")
                .title("Research Funding")
                .description("EU research funding opportunities")
                .build();

        when(blacklistCache.isBlacklisted("funding.research.edu.eu")).thenReturn(false);

        // When
        consumer.consumeSearchResult(event, acknowledgment);

        // Then
        verify(blacklistCache).isBlacklisted("funding.research.edu.eu");
        verify(kafkaTemplate).send(eq("search-results-validated"), any());
        verify(acknowledgment).acknowledge();
    }
}
