package com.northstar.funding.querygeneration.config;

import dev.langchain4j.http.client.jdk.JdkHttpClient;
import dev.langchain4j.http.client.jdk.JdkHttpClientBuilder;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.http.HttpClient;
import java.time.Duration;

/**
 * Configuration for LLM integration (LM Studio or Ollama).
 *
 * <p>LM Studio is the PRIMARY provider for NorthStar workflows because:
 * <ul>
 *   <li>Proven reliability with Perplexica integration</li>
 *   <li>Works correctly despite not claiming concurrent support</li>
 *   <li>Ollama claimed OLLAMA_NUM_PARALLEL=10 but failed in practice with Perplexica</li>
 * </ul>
 *
 * <p>Both LM Studio and Ollama expose OpenAI-compatible APIs, so we use
 * LangChain4j's OpenAiChatModel with HTTP/1.1 for compatibility.
 *
 * <p>Configuration:
 * <ul>
 *   <li>LM Studio: http://192.168.1.10:1234/v1 (PRIMARY)</li>
 *   <li>Ollama: http://192.168.1.10:11434/v1 (FALLBACK)</li>
 * </ul>
 */
@Configuration
public class LlmConfig {

    private static final Logger log = LoggerFactory.getLogger(LlmConfig.class);

    @Value("${query-generation.llm.base-url:http://192.168.1.10:1234/v1}")
    private String baseUrl;

    @Value("${query-generation.llm.api-key:not-needed}")
    private String apiKey;

    @Value("${query-generation.llm.timeout-seconds:60}")
    private int timeoutSeconds;

    @Value("${query-generation.llm.model-name:}")
    private String modelName;

    @Value("${query-generation.llm.max-tokens:500}")
    private int maxTokens;

    @Value("${query-generation.llm.temperature:0.7}")
    private double temperature;

    @Value("${query-generation.llm.log-requests:false}")
    private boolean logRequests;

    @Value("${query-generation.llm.log-responses:false}")
    private boolean logResponses;

    /**
     * Creates LangChain4j ChatModel configured for LM Studio (or Ollama).
     *
     * <p>Key configuration:
     * <ul>
     *   <li>HTTP/1.1: Compatible with both LM Studio and Ollama OpenAI-compatible APIs</li>
     *   <li>60s timeout: Allows time for query generation with larger models</li>
     *   <li>500 max tokens: Sufficient for generating 5-10 search queries</li>
     *   <li>0.7 temperature: Balance between creativity and consistency</li>
     * </ul>
     *
     * @return Configured chat model
     */
    @Bean
    public ChatModel chatModel() {
        log.info("Configuring LLM: baseUrl={}, model={}, timeout={}s, maxTokens={}",
                baseUrl, modelName.isEmpty() ? "(server default)" : modelName, timeoutSeconds, maxTokens);

        // Using HTTP/1.1 for compatibility with LM Studio and Ollama
        HttpClient.Builder httpClientBuilder = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(10));

        JdkHttpClientBuilder jdkHttpClientBuilder = JdkHttpClient.builder()
                .httpClientBuilder(httpClientBuilder);

        OpenAiChatModel.OpenAiChatModelBuilder builder = OpenAiChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .httpClientBuilder(jdkHttpClientBuilder)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .maxTokens(maxTokens)
                .temperature(temperature)
                .logRequests(logRequests)
                .logResponses(logResponses);

        // Only set model name if explicitly configured
        // LM Studio uses whatever model is loaded; Ollama requires model name
        if (modelName != null && !modelName.isEmpty()) {
            builder.modelName(modelName);
        }

        ChatModel chatModel = builder.build();

        log.info("LLM ChatModel configured successfully for {}",
                baseUrl.contains(":1234") ? "LM Studio" : "Ollama");

        return chatModel;
    }
}
