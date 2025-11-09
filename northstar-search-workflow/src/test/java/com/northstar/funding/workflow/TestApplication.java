package com.northstar.funding.workflow;

import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Test application for integration tests.
 *
 * <p>Provides @SpringBootConfiguration for tests.
 * Component scan will pick up all @Service classes in workflow package.
 */
@SpringBootApplication(scanBasePackages = {
        "com.northstar.funding.workflow",
        "com.northstar.funding.persistence"
})
public class TestApplication {
    // Spring Boot test configuration
}
