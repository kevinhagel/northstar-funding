package com.northstar.funding.application;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * NorthStar Funding Discovery Platform - Main Application
 *
 * <p>This is the main entry point for the NorthStar application that aggregates
 * all modules including REST API, search workflow, query generation, and persistence.
 *
 * <p>Access points:
 * <ul>
 *   <li>Swagger UI: http://localhost:8090/swagger-ui.html</li>
 *   <li>OpenAPI JSON: http://localhost:8090/v3/api-docs</li>
 *   <li>Health Check: http://localhost:8090/actuator/health</li>
 * </ul>
 */
@SpringBootApplication(scanBasePackages = {
        "com.northstar.funding.application",    // Application configuration
        "com.northstar.funding.rest",           // REST API controllers
        "com.northstar.funding.kafka",          // Kafka configuration and events
        "com.northstar.funding.querygeneration", // AI query generation
        "com.northstar.funding.persistence",    // Database repositories and services
        "com.northstar.funding.crawler",        // Search result processing
        "com.northstar.funding.search",         // Search adapters (SearXNG, etc.)
        "com.northstar.funding.workflow"        // Kafka workflow consumers
})
public class NorthStarApplication {

    public static void main(String[] args) {
        SpringApplication.run(NorthStarApplication.class, args);
    }
}
