package com.northstar.funding.workflow.kafka;

import com.northstar.funding.kafka.events.SearchRequestEvent;
import com.northstar.funding.search.adapter.SearchAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Test for SearchRequestConsumer (Kafka consumer for search-requests topic).
 *
 * <p>Tests search request processing, adapter delegation, result publishing,
 * acknowledgment handling, and error scenarios.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SearchRequestConsumerTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Mock
    private Acknowledgment acknowledgment;

    private SearchRequestConsumer consumer;

    // Note: searchAdapters is a concrete List, not mocked

    @Test
    void consumeSearchRequest_whenValidRequest_shouldProcessAndPublishResults() {
        // Given
        UUID sessionId = UUID.randomUUID();
        SearchRequestEvent event = SearchRequestEvent.builder()
                .sessionId(sessionId)
                .query("Bulgaria education grants")
                .maxResults(25)
                .build();

        SearchAdapter mockAdapter = mock(SearchAdapter.class);
        when(mockAdapter.isAvailable()).thenReturn(true);
        when(mockAdapter.search(anyString(), anyInt()))
                .thenReturn(List.of(
                        new SearchAdapter.SearchResult(
                                "https://education.gov.bg/grants",
                                "Bulgarian Education Grants",
                                "Apply for education grants in Bulgaria"
                        )
                ));

        consumer = new SearchRequestConsumer(List.of(mockAdapter), kafkaTemplate);

        // When
        consumer.consumeSearchRequest(event, acknowledgment);

        // Then
        verify(mockAdapter).search("Bulgaria education grants", 25);
        verify(kafkaTemplate).send(eq("search-results-raw"), any());
        verify(acknowledgment).acknowledge();
    }

    @Test
    void consumeSearchRequest_whenNoAdaptersAvailable_shouldLogWarningAndAcknowledge() {
        // Given
        SearchRequestEvent event = SearchRequestEvent.builder()
                .sessionId(UUID.randomUUID())
                .query("test query")
                .maxResults(10)
                .build();

        consumer = new SearchRequestConsumer(List.of(), kafkaTemplate);

        // When
        consumer.consumeSearchRequest(event, acknowledgment);

        // Then
        verify(kafkaTemplate, never()).send(anyString(), any());
        verify(acknowledgment).acknowledge(); // Should still acknowledge to avoid reprocessing
    }

    @Test
    void consumeSearchRequest_whenAdapterThrowsException_shouldPublishErrorAndAcknowledge() {
        // Given
        UUID sessionId = UUID.randomUUID();
        SearchRequestEvent event = SearchRequestEvent.builder()
                .sessionId(sessionId)
                .query("test query")
                .maxResults(10)
                .build();

        SearchAdapter mockAdapter = mock(SearchAdapter.class);
        when(mockAdapter.isAvailable()).thenReturn(true);
        when(mockAdapter.search(anyString(), anyInt()))
                .thenThrow(new RuntimeException("Search API timeout"));

        consumer = new SearchRequestConsumer(List.of(mockAdapter), kafkaTemplate);

        // When
        consumer.consumeSearchRequest(event, acknowledgment);

        // Then
        verify(kafkaTemplate).send(eq("workflow-errors"), any());
        verify(acknowledgment).acknowledge();
    }

    @Test
    void consumeSearchRequest_whenMultipleAdapters_shouldProcessAll() {
        // Given
        UUID sessionId = UUID.randomUUID();
        SearchRequestEvent event = SearchRequestEvent.builder()
                .sessionId(sessionId)
                .query("Bulgaria grants")
                .maxResults(10)
                .build();

        SearchAdapter adapter1 = mock(SearchAdapter.class);
        SearchAdapter adapter2 = mock(SearchAdapter.class);

        when(adapter1.isAvailable()).thenReturn(true);
        when(adapter2.isAvailable()).thenReturn(true);

        when(adapter1.search(anyString(), anyInt()))
                .thenReturn(List.of(new SearchAdapter.SearchResult("http://test1.com", "Title 1", "Desc 1")));
        when(adapter2.search(anyString(), anyInt()))
                .thenReturn(List.of(new SearchAdapter.SearchResult("http://test2.com", "Title 2", "Desc 2")));

        consumer = new SearchRequestConsumer(List.of(adapter1, adapter2), kafkaTemplate);

        // When
        consumer.consumeSearchRequest(event, acknowledgment);

        // Then
        verify(adapter1).search("Bulgaria grants", 10);
        verify(adapter2).search("Bulgaria grants", 10);
        verify(kafkaTemplate, times(2)).send(eq("search-results-raw"), any());
        verify(acknowledgment).acknowledge();
    }

    @Test
    void consumeSearchRequest_whenAdapterUnavailable_shouldSkipAdapter() {
        // Given
        SearchRequestEvent event = SearchRequestEvent.builder()
                .sessionId(UUID.randomUUID())
                .query("test query")
                .maxResults(10)
                .build();

        SearchAdapter unavailableAdapter = mock(SearchAdapter.class);
        when(unavailableAdapter.isAvailable()).thenReturn(false);

        consumer = new SearchRequestConsumer(List.of(unavailableAdapter), kafkaTemplate);

        // When
        consumer.consumeSearchRequest(event, acknowledgment);

        // Then
        verify(unavailableAdapter, never()).search(anyString(), anyInt());
        verify(kafkaTemplate, never()).send(anyString(), any());
        verify(acknowledgment).acknowledge();
    }

    @Test
    void consumeSearchRequest_whenNullEvent_shouldHandleGracefully() {
        // Given
        consumer = new SearchRequestConsumer(List.of(), kafkaTemplate);

        // When/Then - should not throw exception
        assertThatCode(() -> consumer.consumeSearchRequest(null, acknowledgment))
                .doesNotThrowAnyException();
        verify(acknowledgment).acknowledge();
    }

    @Test
    void consumeSearchRequest_whenEmptyResults_shouldNotPublish() {
        // Given
        SearchRequestEvent event = SearchRequestEvent.builder()
                .sessionId(UUID.randomUUID())
                .query("nonexistent query")
                .maxResults(10)
                .build();

        SearchAdapter mockAdapter = mock(SearchAdapter.class);
        when(mockAdapter.isAvailable()).thenReturn(true);
        when(mockAdapter.search(anyString(), anyInt())).thenReturn(List.of());

        consumer = new SearchRequestConsumer(List.of(mockAdapter), kafkaTemplate);

        // When
        consumer.consumeSearchRequest(event, acknowledgment);

        // Then
        verify(mockAdapter).search(anyString(), anyInt());
        verify(kafkaTemplate, never()).send(anyString(), any()); // No results to publish
        verify(acknowledgment).acknowledge();
    }
}
