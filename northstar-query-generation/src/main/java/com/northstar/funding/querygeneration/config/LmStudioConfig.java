package com.northstar.funding.querygeneration.config;

import dev.langchain4j.http.client.jdk.JdkHttpClient;
import dev.langchain4j.http.client.jdk.JdkHttpClientBuilder;
import dev.langchain4j.model.chat.ChatModel;
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
     * Creates LangChain4j ChatModel configured for LM Studio.
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
    public ChatModel chatModel() {
        // CRITICAL: LM Studio requires HTTP/1.1 (not HTTP/2)
        HttpClient.Builder httpClientBuilder = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(10));

        // Wrap in JdkHttpClientBuilder to pass to OpenAiChatModel
        JdkHttpClientBuilder jdkHttpClientBuilder = JdkHttpClient.builder()
                .httpClientBuilder(httpClientBuilder);

        return OpenAiChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)  // LM Studio doesn't validate API key, but LangChain4j requires it
                .modelName(modelName)
                .httpClientBuilder(jdkHttpClientBuilder)  // Pass custom HTTP client with HTTP/1.1
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .maxTokens(150)  // Limit response size for faster generation (query generation doesn't need long responses)
                .temperature(0.7)  // Some randomness for query variety
                .logRequests(true)   // Enable for debugging
                .logResponses(true)  // Enable for debugging
                .build();
    }
}
