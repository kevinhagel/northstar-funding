package com.northstar.funding.discovery.search.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * TDD Test for SearchEngineType enum (Feature 003)
 *
 * CRITICAL: This test MUST FAIL before implementing SearchEngineType.java
 *
 * Tests:
 * - Enum values (SEARXNG, TAVILY, PERPLEXITY, BROWSERBASE)
 * - fromString() method (case-insensitive, invalid input)
 * - Display names for UI (future use)
 * - requiresApiKey property
 *
 * Constitutional Compliance:
 * - Enum with properties and methods
 * - Defensive programming for invalid inputs
 */
class SearchEngineTypeTest {

    @Test
    void testAllEngineTypesExist() {
        // Given/When/Then
        assertThat(SearchEngineType.values()).hasSize(4);
        assertThat(SearchEngineType.SEARXNG).isNotNull();
        assertThat(SearchEngineType.BROWSERBASE).isNotNull();
        assertThat(SearchEngineType.TAVILY).isNotNull();
        assertThat(SearchEngineType.PERPLEXITY).isNotNull();
    }

    @Test
    void testSearxngDoesNotRequireApiKey() {
        // Given/When/Then
        assertThat(SearchEngineType.SEARXNG.requiresApiKey()).isFalse();
    }

    @Test
    void testTavilyRequiresApiKey() {
        // Given/When/Then
        assertThat(SearchEngineType.TAVILY.requiresApiKey()).isTrue();
    }

    @Test
    void testPerplexityRequiresApiKey() {
        // Given/When/Then
        assertThat(SearchEngineType.PERPLEXITY.requiresApiKey()).isTrue();
    }

    @Test
    void testBrowserbaseRequiresApiKey() {
        // Given/When/Then
        assertThat(SearchEngineType.BROWSERBASE.requiresApiKey()).isTrue();
    }

    @Test
    void testDisplayNamesAreUserFriendly() {
        // Given/When/Then
        assertThat(SearchEngineType.SEARXNG.getDisplayName()).isEqualTo("Searxng");
        assertThat(SearchEngineType.TAVILY.getDisplayName()).isEqualTo("Tavily");
        assertThat(SearchEngineType.PERPLEXITY.getDisplayName()).isEqualTo("Perplexity");
        assertThat(SearchEngineType.BROWSERBASE.getDisplayName()).isEqualTo("Browserbase");
    }

    @Test
    void testDefaultBaseUrlsAreConfigured() {
        // Given/When/Then
        assertThat(SearchEngineType.SEARXNG.getDefaultBaseUrl())
            .isEqualTo("http://192.168.1.10:8080");
        assertThat(SearchEngineType.TAVILY.getDefaultBaseUrl())
            .isEqualTo("https://api.tavily.com");
        assertThat(SearchEngineType.PERPLEXITY.getDefaultBaseUrl())
            .isEqualTo("https://api.perplexity.ai");
        assertThat(SearchEngineType.BROWSERBASE.getDefaultBaseUrl())
            .isEqualTo("https://api.browserbase.com");
    }

    @Test
    void testFromStringCaseInsensitive() {
        // Given/When/Then
        assertThat(SearchEngineType.fromString("searxng")).isEqualTo(SearchEngineType.SEARXNG);
        assertThat(SearchEngineType.fromString("SEARXNG")).isEqualTo(SearchEngineType.SEARXNG);
        assertThat(SearchEngineType.fromString("SeArXnG")).isEqualTo(SearchEngineType.SEARXNG);
        assertThat(SearchEngineType.fromString("tavily")).isEqualTo(SearchEngineType.TAVILY);
        assertThat(SearchEngineType.fromString("PERPLEXITY")).isEqualTo(SearchEngineType.PERPLEXITY);
        assertThat(SearchEngineType.fromString("browserbase")).isEqualTo(SearchEngineType.BROWSERBASE);
    }

    @Test
    void testFromStringThrowsExceptionForInvalidInput() {
        // Given/When/Then
        assertThatThrownBy(() -> SearchEngineType.fromString("google"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Unknown search engine type: google");
    }

    @Test
    void testFromStringThrowsExceptionForNullInput() {
        // Given/When/Then
        assertThatThrownBy(() -> SearchEngineType.fromString(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("value");
    }

    @Test
    void testFromStringThrowsExceptionForBlankInput() {
        // Given/When/Then
        assertThatThrownBy(() -> SearchEngineType.fromString("   "))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("value cannot be blank");
    }

    @Test
    void testEnumToStringMatchesName() {
        // Given/When/Then
        assertThat(SearchEngineType.SEARXNG.toString()).isEqualTo("SEARXNG");
        assertThat(SearchEngineType.TAVILY.toString()).isEqualTo("TAVILY");
        assertThat(SearchEngineType.PERPLEXITY.toString()).isEqualTo("PERPLEXITY");
        assertThat(SearchEngineType.BROWSERBASE.toString()).isEqualTo("BROWSERBASE");
    }
}
