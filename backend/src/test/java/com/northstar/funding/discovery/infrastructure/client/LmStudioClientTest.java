package com.northstar.funding.discovery.infrastructure.client;

import com.northstar.funding.discovery.infrastructure.config.LmStudioProperties;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for LM Studio client.
 *
 * Tests:
 * 1. Connection to LM Studio on Mac Studio (192.168.1.10:1234)
 * 2. Model availability
 * 3. Text generation for query generation use case
 *
 * Prerequisites:
 * - LM Studio running on Mac Studio
 * - At least one model loaded (llama-3.1-8b-instruct or phi-3-medium-4k-instruct)
 *
 * Constitutional Compliance:
 * - Local AI only (no external LLM calls)
 * - Mac Studio infrastructure
 */
@SpringBootTest
@ActiveProfiles("default")
@Slf4j
class LmStudioClientTest {

    @Autowired
    private LmStudioClient lmStudioClient;

    @Autowired
    private LmStudioProperties lmStudioProperties;

    @BeforeEach
    void setUp() {
        log.info("Testing LM Studio connection at: {}", lmStudioProperties.getBaseUrl());
        log.info("Using model: {}", lmStudioProperties.getModel());
    }

    @Test
    void testLmStudioIsAvailable() {
        // When
        boolean isAvailable = lmStudioClient.isAvailable();

        // Then
        assertThat(isAvailable)
            .as("LM Studio should be available at %s", lmStudioProperties.getBaseUrl())
            .isTrue();

        log.info("✅ LM Studio is available");
    }

    @Test
    void testGetAvailableModels() {
        // When
        List<String> models = lmStudioClient.getAvailableModels();

        // Then
        assertThat(models)
            .as("LM Studio should have at least one model loaded")
            .isNotEmpty();

        assertThat(models)
            .as("Expected llama-3.1-8b-instruct or phi-3-medium-4k-instruct to be available")
            .anyMatch(model -> model.contains("llama") || model.contains("phi"));

        log.info("✅ Available models: {}", models);
    }

    @Test
    void testGenerateSearchQueries() {
        // Given
        String prompt = """
            Generate 3 search queries for finding education grants in Bulgaria.
            Format: One query per line, no numbering, no extra explanation.
            Focus: NGOs, foundations, EU funding.
            """;

        // When
        String response = lmStudioClient.generate(prompt);

        // Then
        assertThat(response)
            .as("LM Studio should generate a non-empty response")
            .isNotBlank();

        String[] lines = response.split("\n");
        assertThat(lines)
            .as("Response should contain at least 2 search queries")
            .hasSizeGreaterThanOrEqualTo(2);

        log.info("✅ Generated queries:");
        for (String query : lines) {
            if (!query.isBlank()) {
                log.info("   - {}", query.trim());
            }
        }
    }

    @Test
    void testGenerateWithShortPrompt() {
        // Given
        String prompt = "What is the capital of Bulgaria?";

        // When
        String response = lmStudioClient.generate(prompt);

        // Then
        assertThat(response)
            .as("LM Studio should generate a response")
            .isNotBlank()
            .as("Response should mention Sofia")
            .containsIgnoringCase("Sofia");

        log.info("✅ Short prompt response: {}", response);
    }

    @Test
    void testGenerateFundingTypeQuery() {
        // Given
        String prompt = """
            Create a search query to find grant opportunities for educational NGOs in Eastern Europe.
            Return only the search query, no explanation.
            """;

        // When
        String response = lmStudioClient.generate(prompt);

        // Then
        assertThat(response)
            .as("Response should contain funding-related keywords")
            .containsAnyOf("grant", "grants", "funding", "scholarship", "education", "NGO", "Eastern Europe");

        log.info("✅ Funding query generated: {}", response.trim());
    }
}
