package com.northstar.funding.querygeneration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Test application for integration tests.
 *
 * <p>This class provides the Spring Boot context for @SpringBootTest integration tests.
 *
 * <p>Repository configuration is automatically picked up from PersistenceConfiguration
 * via component scanning. Do NOT add @EnableJdbcRepositories here as it would cause
 * duplicate bean registration errors.
 */
@SpringBootApplication(scanBasePackages = {
        "com.northstar.funding.querygeneration",
        "com.northstar.funding.persistence"
})
public class TestApplication {

    public static void main(String[] args) {
        SpringApplication.run(TestApplication.class, args);
    }
}
