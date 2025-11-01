package com.northstar.funding.discovery.infrastructure.config;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for LM Studio AI integration.
 *
 * LM Studio provides an OpenAI-compatible API running locally on Mac Studio.
 * Constitutional Requirement: Local AI, no external LLM dependencies.
 *
 * @see <a href="http://192.168.1.10:1234/v1">LM Studio API</a>
 */
@Component
@ConfigurationProperties(prefix = "lm-studio")
@Data
@ToString
@Slf4j
public class LmStudioProperties {

    /**
     * Base URL for LM Studio API (OpenAI-compatible).
     * Default: http://192.168.1.10:1234/v1
     */
    private String baseUrl = "http://192.168.1.10:1234/v1";

    /**
     * Model name to use for query generation.
     * Available models: llama-3.1-8b-instruct, phi-3-medium-4k-instruct
     */
    private String model = "llama-3.1-8b-instruct";

    /**
     * Request timeout in seconds.
     */
    private int timeoutSeconds = 30;

    /**
     * Maximum tokens to generate in response.
     */
    private int maxTokens = 500;

    /**
     * Temperature for response generation (0.0-1.0).
     * Lower = more deterministic, Higher = more creative.
     */
    private double temperature = 0.7;

    @PostConstruct
    public void logConfiguration() {
        log.info("🔧 LM Studio Configuration:");
        log.info("  - Base URL: {}", baseUrl);
        log.info("  - Model: {}", model);
        log.info("  - Temperature: {}", temperature);
        log.info("  - Max Tokens: {}", maxTokens);
        log.info("  - Timeout: {}s", timeoutSeconds);

        validateConfiguration();
    }

    private void validateConfiguration() {
        if (baseUrl == null || baseUrl.isBlank()) {
            log.error("❌ LM Studio base URL is not configured!");
        } else {
            log.info("✅ LM Studio configuration valid");
        }
    }

    /**
     * Check if LM Studio is properly configured.
     *
     * @return true if base URL and model are configured
     */
    public boolean isConfigured() {
        return baseUrl != null && !baseUrl.isBlank() &&
               model != null && !model.isBlank();
    }
}
