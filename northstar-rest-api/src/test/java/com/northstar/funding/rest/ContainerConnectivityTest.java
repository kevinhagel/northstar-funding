package com.northstar.funding.rest;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Minimal test to verify TestContainers can start PostgreSQL and Kafka containers.
 * This test validates Docker connectivity and container startup before running
 * actual integration tests.
 */
public class ContainerConnectivityTest extends AbstractIntegrationTest {

    @Test
    void containers_ShouldStartSuccessfully() {
        // Verify PostgreSQL container is running
        assertThat(postgres.isRunning())
            .as("PostgreSQL container should be running")
            .isTrue();

        // Verify Kafka container is running
        assertThat(kafka.isRunning())
            .as("Kafka container should be running")
            .isTrue();

        // Verify PostgreSQL JDBC URL is accessible
        assertThat(postgres.getJdbcUrl())
            .as("PostgreSQL JDBC URL should not be null")
            .isNotNull()
            .startsWith("jdbc:postgresql://");

        // Verify Kafka bootstrap servers are accessible
        assertThat(kafka.getBootstrapServers())
            .as("Kafka bootstrap servers should not be null")
            .isNotNull()
            .contains("localhost:");

        System.out.println("✅ PostgreSQL container running at: " + postgres.getJdbcUrl());
        System.out.println("✅ Kafka container (KRaft mode) running at: " + kafka.getBootstrapServers());
    }
}
