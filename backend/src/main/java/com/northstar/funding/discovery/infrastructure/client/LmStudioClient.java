package com.northstar.funding.discovery.infrastructure.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.northstar.funding.discovery.infrastructure.config.LmStudioProperties;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;

/**
 * HTTP client for LM Studio AI integration.
 *
 * LM Studio provides an OpenAI-compatible API running locally on Mac Studio.
 * Constitutional Requirement: Local AI, no external LLM dependencies.
 *
 * CRITICAL FIX: Forces HTTP/1.1 for LM Studio compatibility.
 * LM Studio has problems with HTTP/2 requests - this configuration ensures
 * all requests use HTTP/1.1 protocol.
 *
 * Uses:
 * - RestClient with JDK HttpClient (HTTP/1.1 forced)
 * - Resilience4j for circuit breaker and retry
 * - Virtual Threads for async operations
 *
 * @see LmStudioProperties
 */
@Component
@Slf4j
public class LmStudioClient {

    private final LmStudioProperties properties;
    private final RestClient restClient;

    /**
     * Constructor to initialize LmStudioClient with HTTP/1.1 compatibility.
     *
     * CRITICAL: Configures HttpClient to use HTTP/1.1 for LM Studio compatibility.
     */
    public LmStudioClient(LmStudioProperties properties) {
        this.properties = properties;

        // CRITICAL FIX: Force HTTP/1.1 for LM Studio compatibility
        // LM Studio has issues with HTTP/2 - force HTTP/1.1 for all requests
        HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)  // Force HTTP/1.1
            .connectTimeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);

        this.restClient = RestClient.builder()
            .baseUrl(properties.getBaseUrl())
            .requestFactory(requestFactory)
            .build();

        log.info("🔧 LM Studio HTTP client configured with HTTP/1.1 (LM Studio compatibility fix)");
    }

    @PostConstruct
    public void logConfiguration() {
        log.info("🚀 LM Studio Client Initialized:");
        log.info("  - Base URL: {}", properties.getBaseUrl());
        log.info("  - Model: {}", properties.getModel());
        log.info("  - Temperature: {}", properties.getTemperature());
        log.info("  - Max Tokens: {}", properties.getMaxTokens());
        log.info("  - Timeout: {}s", properties.getTimeoutSeconds());
        log.info("  - HTTP Version: 1.1 (forced for LM Studio compatibility)");
    }

    /**
     * Generate text completion from a prompt.
     *
     * @param prompt The input prompt for text generation
     * @return Generated text response
     */
    @CircuitBreaker(name = "lmStudio", fallbackMethod = "generateFallback")
    @Retry(name = "lmStudio")
    public String generate(String prompt) {
        log.debug("Calling LM Studio with prompt: {}", prompt.substring(0, Math.min(50, prompt.length())));

        ChatCompletionRequest request = new ChatCompletionRequest();
        request.setModel(properties.getModel());
        request.setMaxTokens(properties.getMaxTokens());
        request.setTemperature(properties.getTemperature());
        request.setMessages(List.of(new Message("user", prompt)));

        try {
            ChatCompletionResponse response = restClient.post()
                .uri("/chat/completions")
                .body(request)
                .retrieve()
                .body(ChatCompletionResponse.class);

            if (response == null || response.getChoices() == null || response.getChoices().isEmpty()) {
                log.warn("LM Studio returned empty response");
                return "";
            }

            String generatedText = response.getChoices().get(0).getMessage().getContent();
            log.debug("LM Studio response: {}", generatedText.substring(0, Math.min(50, generatedText.length())));

            return generatedText;
        } catch (Exception e) {
            log.error("Error calling LM Studio API: {}", e.getMessage(), e);
            throw new LmStudioException("Failed to generate text from LM Studio", e);
        }
    }

    /**
     * Fallback method for circuit breaker.
     */
    private String generateFallback(String prompt, Throwable t) {
        log.warn("LM Studio circuit breaker triggered, using fallback. Error: {}", t.getMessage());
        return ""; // Return empty string as fallback
    }

    /**
     * Check if LM Studio is available.
     *
     * @return true if LM Studio API is reachable
     */
    public boolean isAvailable() {
        try {
            ModelsResponse response = restClient.get()
                .uri("/models")
                .retrieve()
                .body(ModelsResponse.class);

            boolean available = response != null && response.getData() != null && !response.getData().isEmpty();
            log.info("LM Studio availability check: {} (models found: {})",
                available ? "AVAILABLE" : "UNAVAILABLE",
                response != null ? response.getData().size() : 0);

            return available;
        } catch (Exception e) {
            log.warn("LM Studio is not available: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Get list of available models.
     *
     * @return List of model IDs
     */
    public List<String> getAvailableModels() {
        try {
            ModelsResponse response = restClient.get()
                .uri("/models")
                .retrieve()
                .body(ModelsResponse.class);

            if (response == null || response.getData() == null) {
                return List.of();
            }

            return response.getData().stream()
                .map(ModelData::getId)
                .toList();
        } catch (Exception e) {
            log.error("Error fetching available models: {}", e.getMessage());
            return List.of();
        }
    }

    // ===== DTOs for OpenAI-compatible API =====

    @Getter
    @Setter
    public static class ChatCompletionRequest {
        private String model;
        private List<Message> messages;
        private double temperature;
        @JsonProperty("max_tokens")
        private int maxTokens;
    }

    @Getter
    @Setter
    public static class Message {
        private String role;
        private String content;

        public Message() {}

        public Message(String role, String content) {
            this.role = role;
            this.content = content;
        }
    }

    @Getter
    @Setter
    public static class ChatCompletionResponse {
        private String id;
        private String object;
        private long created;
        private String model;
        private List<Choice> choices;
        private Usage usage;
    }

    @Getter
    @Setter
    public static class Choice {
        private int index;
        private Message message;
        @JsonProperty("finish_reason")
        private String finishReason;
    }

    @Getter
    @Setter
    public static class Usage {
        @JsonProperty("prompt_tokens")
        private int promptTokens;
        @JsonProperty("completion_tokens")
        private int completionTokens;
        @JsonProperty("total_tokens")
        private int totalTokens;
    }

    @Getter
    @Setter
    public static class ModelsResponse {
        private String object;
        private List<ModelData> data;
    }

    @Getter
    @Setter
    public static class ModelData {
        private String id;
        private String object;
        private long created;
        @JsonProperty("owned_by")
        private String ownedBy;
    }

    /**
     * Custom exception for LM Studio client errors.
     */
    public static class LmStudioException extends RuntimeException {
        public LmStudioException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
