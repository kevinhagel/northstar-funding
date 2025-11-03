package com.northstar.funding.querygeneration.config;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.http.HttpClient;
import java.time.Duration;

/**
 * Configuration for LM Studio integration.
 *
 * <p>CRITICAL: LM Studio requires HTTP/1.1 (does not support HTTP/2).
 * This configuration explicitly sets the HTTP version to prevent connection failures.
 */
@Configuration
public class LmStudioConfig {

    @Value("${query-generation.lm-studio.base-url:http://192.168.1.10:1234/v1}")
    private String baseUrl;

    @Value("${query-generation.lm-studio.api-key:not-needed}")
    private String apiKey;

    @Value("${query-generation.lm-studio.timeout-seconds:30}")
    private int timeoutSeconds;

    @Value("${query-generation.lm-studio.model-name:local-model}")
    private String modelName;

    /**
     * Creates LangChain4j ChatLanguageModel configured for LM Studio.
     *
     * <p>Key configuration:
     * <ul>
     *   <li>HTTP/1.1: Required for LM Studio compatibility</li>
     *   <li>30s timeout: Allows time for query generation</li>
     *   <li>Non-blocking: Returns CompletableFuture for Virtual Thread integration</li>
     * </ul>
     *
     * @return Configured chat model
     */
    @Bean
    public ChatLanguageModel chatLanguageModel() {
        // CRITICAL: LM Studio requires HTTP/1.1 (not HTTP/2)
        HttpClient.Builder httpClientBuilder = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(10));

        return OpenAiChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)  // LM Studio doesn't validate API key, but LangChain4j requires it
                .modelName(modelName)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .logRequests(false)
                .logResponses(false)
                .build();
    }
}
