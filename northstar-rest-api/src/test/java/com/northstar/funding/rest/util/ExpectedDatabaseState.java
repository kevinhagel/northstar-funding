package com.northstar.funding.rest.util;

import com.northstar.funding.domain.SessionStatus;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Utility class for asserting expected database state in integration tests.
 * Provides fluent assertions for common database verification scenarios.
 */
public class ExpectedDatabaseState {

    /**
     * Assert that a discovery session exists in the database with correct metadata.
     *
     * @param sessionId     The session ID to verify
     * @param jdbcTemplate  JDBC template for database queries
     */
    public static void assertSessionCreated(UUID sessionId, JdbcTemplate jdbcTemplate) {
        // Verify session exists
        String countSql = "SELECT COUNT(*) FROM discovery_session WHERE session_id = ?";
        Integer count = jdbcTemplate.queryForObject(countSql, Integer.class, sessionId);
        assertThat(count)
            .as("Session %s should exist in database", sessionId)
            .isEqualTo(1);

        // Verify session status
        String statusSql = "SELECT status FROM discovery_session WHERE session_id = ?";
        String status = jdbcTemplate.queryForObject(statusSql, String.class, sessionId);
        assertThat(status)
            .as("Session status should be RUNNING")
            .isEqualTo("RUNNING");

        // Verify session type
        String typeSql = "SELECT session_type FROM discovery_session WHERE session_id = ?";
        String sessionType = jdbcTemplate.queryForObject(typeSql, String.class, sessionId);
        assertThat(sessionType)
            .as("Session type should be MANUAL for REST API requests")
            .isEqualTo("MANUAL");

        // Verify created_at is recent (within last 10 seconds)
        String createdAtSql = "SELECT created_at FROM discovery_session WHERE session_id = ?";
        Instant createdAt = jdbcTemplate.queryForObject(createdAtSql, Instant.class, sessionId);
        assertThat(createdAt)
            .as("Session created_at should be recent")
            .isNotNull()
            .isAfter(Instant.now().minus(10, ChronoUnit.SECONDS));
    }

    /**
     * Assert that NO discovery sessions exist in the database.
     * Useful for verifying that invalid requests don't create database records.
     *
     * @param jdbcTemplate JDBC template for database queries
     */
    public static void assertNoSessionCreated(JdbcTemplate jdbcTemplate) {
        String sql = "SELECT COUNT(*) FROM discovery_session";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class);
        assertThat(count)
            .as("No sessions should exist in database after invalid request")
            .isEqualTo(0);
    }

    /**
     * Assert that a session has a specific status.
     *
     * @param sessionId      The session ID to verify
     * @param expectedStatus The expected session status
     * @param jdbcTemplate   JDBC template for database queries
     */
    public static void assertSessionStatus(UUID sessionId, SessionStatus expectedStatus, JdbcTemplate jdbcTemplate) {
        String sql = "SELECT status FROM discovery_session WHERE session_id = ?";
        String status = jdbcTemplate.queryForObject(sql, String.class, sessionId);
        assertThat(status)
            .as("Session %s should have status %s", sessionId, expectedStatus)
            .isEqualTo(expectedStatus.name());
    }

    /**
     * Count sessions in the database.
     * Useful for verifying exact session counts in concurrent request tests.
     *
     * @param jdbcTemplate JDBC template for database queries
     * @return Number of sessions in database
     */
    public static int countSessions(JdbcTemplate jdbcTemplate) {
        String sql = "SELECT COUNT(*) FROM discovery_session";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class);
        return count != null ? count : 0;
    }
}
