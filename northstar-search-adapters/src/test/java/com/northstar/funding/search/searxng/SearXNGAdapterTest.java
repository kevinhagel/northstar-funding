package com.northstar.funding.search.searxng;

import com.northstar.funding.domain.SearchEngineType;
import com.northstar.funding.search.adapter.SearchAdapter;
import com.northstar.funding.search.config.SearchAdapterConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Test for SearXNG search adapter.
 *
 * <p>Tests search execution, JSON parsing, error handling, and availability checking.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SearXNGAdapterTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private SearchAdapterConfig config;

    private SearXNGAdapter adapter;

    @BeforeEach
    void setUp() {
        when(config.getSearxngBaseUrl()).thenReturn("http://192.168.1.10:8080");
        when(config.getSearxngTimeoutSeconds()).thenReturn(10);

        adapter = new SearXNGAdapter(restTemplate, config);
    }

    @Test
    void getEngineType_shouldReturnSearXNG() {
        // When
        SearchEngineType type = adapter.getEngineType();

        // Then
        assertThat(type).isEqualTo(SearchEngineType.SEARXNG);
    }

    @Test
    void search_withValidQuery_shouldReturnResults() {
        // Given
        String query = "Bulgaria education grants";
        int maxResults = 10;

        String mockResponse = """
                {
                  "query": "Bulgaria education grants",
                  "results": [
                    {
                      "url": "https://education.gov.bg/grants",
                      "title": "Bulgarian Education Grants",
                      "content": "Apply for education grants in Bulgaria"
                    },
                    {
                      "url": "https://eu.europa.eu/funding",
                      "title": "EU Education Funding",
                      "content": "European funding opportunities for education"
                    }
                  ]
                }
                """;

        when(restTemplate.getForObject(anyString(), eq(String.class)))
                .thenReturn(mockResponse);

        // When
        List<SearchAdapter.SearchResult> results = adapter.search(query, maxResults);

        // Then
        assertThat(results).hasSize(2);
        assertThat(results.get(0).url()).isEqualTo("https://education.gov.bg/grants");
        assertThat(results.get(0).title()).isEqualTo("Bulgarian Education Grants");
        assertThat(results.get(0).description()).isEqualTo("Apply for education grants in Bulgaria");

        assertThat(results.get(1).url()).isEqualTo("https://eu.europa.eu/funding");
        assertThat(results.get(1).title()).isEqualTo("EU Education Funding");

        // Verify correct API call (Spring uses %20 for spaces, not +)
        verify(restTemplate).getForObject(
                eq("http://192.168.1.10:8080/search?q=Bulgaria%20education%20grants&format=json&pageno=1"),
                eq(String.class)
        );
    }

    @Test
    void search_withMaxResults_shouldRespectLimit() {
        // Given
        String mockResponse = """
                {
                  "query": "test",
                  "results": [
                    {"url": "http://result1.com", "title": "Result 1", "content": "Content 1"},
                    {"url": "http://result2.com", "title": "Result 2", "content": "Content 2"},
                    {"url": "http://result3.com", "title": "Result 3", "content": "Content 3"},
                    {"url": "http://result4.com", "title": "Result 4", "content": "Content 4"},
                    {"url": "http://result5.com", "title": "Result 5", "content": "Content 5"}
                  ]
                }
                """;

        when(restTemplate.getForObject(anyString(), eq(String.class)))
                .thenReturn(mockResponse);

        // When
        List<SearchAdapter.SearchResult> results = adapter.search("test", 3);

        // Then - should only return first 3 results
        assertThat(results).hasSize(3);
        assertThat(results.get(0).url()).isEqualTo("http://result1.com");
        assertThat(results.get(2).url()).isEqualTo("http://result3.com");
    }

    @Test
    void search_withEmptyResults_shouldReturnEmptyList() {
        // Given
        String mockResponse = """
                {
                  "query": "nonexistent query",
                  "results": []
                }
                """;

        when(restTemplate.getForObject(anyString(), eq(String.class)))
                .thenReturn(mockResponse);

        // When
        List<SearchAdapter.SearchResult> results = adapter.search("nonexistent query", 10);

        // Then
        assertThat(results).isEmpty();
    }

    @Test
    void search_withMissingFields_shouldHandleGracefully() {
        // Given - some results missing description
        String mockResponse = """
                {
                  "query": "test",
                  "results": [
                    {"url": "http://test.com", "title": "Test"}
                  ]
                }
                """;

        when(restTemplate.getForObject(anyString(), eq(String.class)))
                .thenReturn(mockResponse);

        // When
        List<SearchAdapter.SearchResult> results = adapter.search("test", 10);

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).description()).isEqualTo(""); // Empty string, not null
    }

    @Test
    void search_withHttpError_shouldThrowException() {
        // Given
        when(restTemplate.getForObject(anyString(), eq(String.class)))
                .thenThrow(new RuntimeException("HTTP 500: Server Error"));

        // When/Then
        assertThatThrownBy(() -> adapter.search("test", 10))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("HTTP 500");
    }

    @Test
    void isAvailable_whenSearXNGResponds_shouldReturnTrue() {
        // Given
        when(restTemplate.getForObject(anyString(), eq(String.class)))
                .thenReturn("{\"query\":\"test\",\"results\":[]}");

        // When
        boolean available = adapter.isAvailable();

        // Then
        assertThat(available).isTrue();
    }

    @Test
    void isAvailable_whenSearXNGFails_shouldReturnFalse() {
        // Given
        when(restTemplate.getForObject(anyString(), eq(String.class)))
                .thenThrow(new RuntimeException("Connection refused"));

        // When
        boolean available = adapter.isAvailable();

        // Then
        assertThat(available).isFalse();
    }
}
