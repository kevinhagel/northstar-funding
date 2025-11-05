package com.northstar.funding.crawler;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Test application for crawler integration tests.
 *
 * This minimal Spring Boot application is used to bootstrap the application context
 * for integration tests with TestContainers.
 *
 * Component scanning and JDBC repository configuration are inherited from
 * PersistenceConfiguration in the northstar-persistence module.
 */
@SpringBootApplication(scanBasePackages = {
    "com.northstar.funding.crawler",
    "com.northstar.funding.persistence",
    "com.northstar.funding.domain"
})
public class CrawlerTestApplication {

    public static void main(String[] args) {
        SpringApplication.run(CrawlerTestApplication.class, args);
    }
}
