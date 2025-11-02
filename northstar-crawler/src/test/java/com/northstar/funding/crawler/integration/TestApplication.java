package com.northstar.funding.crawler.integration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * Test Spring Boot application for integration tests.
 *
 * Scans all packages to pick up:
 * - northstar-crawler adapters and services
 * - northstar-persistence repositories and services
 * - northstar-domain entities
 */
@SpringBootApplication
@ComponentScan(basePackages = {
        "com.northstar.funding.crawler",
        "com.northstar.funding.persistence",
        "com.northstar.funding.domain"
})
public class TestApplication {
    public static void main(String[] args) {
        SpringApplication.run(TestApplication.class, args);
    }
}
