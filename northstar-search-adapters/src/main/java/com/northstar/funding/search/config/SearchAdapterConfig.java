package com.northstar.funding.search.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Configuration for search engine adapters.
 */
@Configuration
public class SearchAdapterConfig {

    @Value("${search.searxng.base-url:http://192.168.1.10:8080}")
    private String searxngBaseUrl;

    @Value("${search.searxng.timeout-seconds:10}")
    private int searxngTimeoutSeconds;

    /**
     * RestTemplate for HTTP calls to search engines.
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    public String getSearxngBaseUrl() {
        return searxngBaseUrl;
    }

    public int getSearxngTimeoutSeconds() {
        return searxngTimeoutSeconds;
    }
}
