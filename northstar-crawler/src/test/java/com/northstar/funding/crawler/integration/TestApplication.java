package com.northstar.funding.crawler.integration;

import com.northstar.funding.crawler.SimpleCrawlRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

/**
 * Test Spring Boot application for integration tests.
 *
 * Scans all packages to pick up:
 * - northstar-crawler adapters and services
 * - northstar-persistence repositories and services
 * - northstar-domain entities
 *
 * Excludes SimpleCrawlRunner which is a CLI application with System.exit() calls.
 */
@SpringBootApplication
@ComponentScan(
        basePackages = {
                "com.northstar.funding.crawler",
                "com.northstar.funding.persistence",
                "com.northstar.funding.domain"
        },
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = SimpleCrawlRunner.class
        )
)
public class TestApplication {
    public static void main(String[] args) {
        SpringApplication.run(TestApplication.class, args);
    }
}
