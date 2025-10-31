package com.northstar.funding.discovery.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * RestClient configuration for search engine adapters (Feature 003)
 *
 * Provides RestClient.Builder bean for dependency injection into adapters.
 * RestClient is the modern replacement for deprecated RestTemplate (Spring 6.1+).
 *
 * Constitutional Compliance:
 * - Spring Boot 3.5.6 with Spring 6.1+ RestClient
 * - NO langchain4j (plain HTTP client)
 *
 * @author NorthStar Funding Team
 */
@Configuration
public class RestClientConfig {

    @Bean
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder()
            .requestFactory(new org.springframework.http.client.JdkClientHttpRequestFactory());
    }
}
