package com.northstar.funding.discovery.infrastructure;

import java.math.BigDecimal;

import com.northstar.funding.discovery.domain.Domain;
import com.northstar.funding.discovery.domain.DomainStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for DomainRepository
 *
 * Tests Spring Data JDBC custom queries for domain-level deduplication.
 * Uses TestContainers PostgreSQL with Flyway migrations.
 *
 * Critical TDD Flow:
 * 1. Write this test FIRST (T001)
 * 2. Tests will FAIL (repository not implemented yet)
 * 3. Implement DomainRepository (T002)
 * 4. Tests should PASS
 */
@SpringBootTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Transactional
class DomainRepositoryIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
        .withDatabaseName("northstar_test")
        .withUsername("test_user")
        .withPassword("test_password")
        .withReuse(true);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    }

    @Autowired
    private DomainRepository domainRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private UUID sessionId;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        // @Transactional handles cleanup - no manual cleanup needed
        sessionId = UUID.randomUUID();
        now = LocalDateTime.now();

        // Create discovery_session for FK reference
        jdbcTemplate.execute(String.format(
            "INSERT INTO discovery_session (session_id, started_at, status, candidates_found, duplicates_detected) " +
            "VALUES ('%s', NOW(), 'RUNNING', 0, 0)",
            sessionId
        ));
    }

    // ===== Test findByDomainName - Unique Constraint Lookup =====

    @Test
    void testFindByDomainName_ExistingDomain_ReturnsDomain() {
        // Given: Domain exists in database
        Domain domain = createDomain("example.org", DomainStatus.DISCOVERED);
        domainRepository.save(domain);

        // When: Search by domain name
        Optional<Domain> result = domainRepository.findByDomainName("example.org");

        // Then: Domain is found
        assertThat(result).isPresent();
        assertThat(result.get().getDomainName()).isEqualTo("example.org");
        assertThat(result.get().getStatus()).isEqualTo(DomainStatus.DISCOVERED);
    }

    @Test
    void testFindByDomainName_NonExistentDomain_ReturnsEmpty() {
        // When: Search for non-existent domain
        Optional<Domain> result = domainRepository.findByDomainName("nonexistent.com");

        // Then: Empty result
        assertThat(result).isEmpty();
    }

    @Test
    void testFindByDomainName_CaseSensitive() {
        // Given: Domain exists with lowercase
        Domain domain = createDomain("example.org", DomainStatus.DISCOVERED);
        domainRepository.save(domain);

        // When: Search with uppercase
        Optional<Domain> result = domainRepository.findByDomainName("EXAMPLE.ORG");

        // Then: Should NOT find (case-sensitive)
        assertThat(result).isEmpty();
    }

    // ===== Test existsByDomainName - Fast Boolean Lookup =====

    @Test
    void testExistsByDomainName_ExistingDomain_ReturnsTrue() {
        // Given: Domain exists
        Domain domain = createDomain("example.org", DomainStatus.DISCOVERED);
        domainRepository.save(domain);

        // When: Check existence
        boolean exists = domainRepository.existsByDomainName("example.org");

        // Then: Returns true
        assertThat(exists).isTrue();
    }

    @Test
    void testExistsByDomainName_NonExistentDomain_ReturnsFalse() {
        // When: Check non-existent domain
        boolean exists = domainRepository.existsByDomainName("nonexistent.com");

        // Then: Returns false
        assertThat(exists).isFalse();
    }

    // ===== Test findByStatus - Blacklist Queries =====

    @Test
    void testFindByStatus_Blacklisted_ReturnsOnlyBlacklistedDomains() {
        // Given: Mix of domains with different statuses
        domainRepository.save(createDomain("blacklisted1.com", DomainStatus.BLACKLISTED));
        domainRepository.save(createDomain("blacklisted2.com", DomainStatus.BLACKLISTED));
        domainRepository.save(createDomain("good.org", DomainStatus.PROCESSED_HIGH_QUALITY));
        domainRepository.save(createDomain("pending.net", DomainStatus.DISCOVERED));

        // When: Query for blacklisted domains
        List<Domain> blacklisted = domainRepository.findByStatus(DomainStatus.BLACKLISTED);

        // Then: Only blacklisted domains returned
        assertThat(blacklisted).hasSize(2);
        assertThat(blacklisted)
            .extracting(Domain::getDomainName)
            .containsExactlyInAnyOrder("blacklisted1.com", "blacklisted2.com");
    }

    @Test
    void testFindByStatus_NoMatchingStatus_ReturnsEmpty() {
        // Given: Domains with different statuses
        domainRepository.save(createDomain("example.org", DomainStatus.DISCOVERED));

        // When: Query for status that doesn't exist
        List<Domain> failed = domainRepository.findByStatus(DomainStatus.PROCESSING_FAILED);

        // Then: Empty list
        assertThat(failed).isEmpty();
    }

    // ===== Test findDomainsReadyForRetry - Exponential Backoff =====

    @Test
    void testFindDomainsReadyForRetry_RetryTimePassed_ReturnsDomains() {
        // Given: Domain with retry_after in the past
        Domain domain1 = createDomain("retry1.com", DomainStatus.PROCESSING_FAILED);
        domain1.setRetryAfter(now.minusHours(1)); // 1 hour ago - ready to retry
        domain1.setFailureCount(2);
        domainRepository.save(domain1);

        // Given: Domain with retry_after in the future (not ready)
        Domain domain2 = createDomain("retry2.com", DomainStatus.PROCESSING_FAILED);
        domain2.setRetryAfter(now.plusHours(1)); // 1 hour from now - NOT ready
        domain2.setFailureCount(1);
        domainRepository.save(domain2);

        // Given: Domain with no retry_after (not failed)
        Domain domain3 = createDomain("ok.org", DomainStatus.DISCOVERED);
        domainRepository.save(domain3);

        // When: Query for domains ready to retry
        List<Domain> readyToRetry = domainRepository.findDomainsReadyForRetry(now);

        // Then: Only domain1 is ready
        assertThat(readyToRetry).hasSize(1);
        assertThat(readyToRetry.get(0).getDomainName()).isEqualTo("retry1.com");
        assertThat(readyToRetry.get(0).getFailureCount()).isEqualTo(2);
    }

    @Test
    void testFindDomainsReadyForRetry_NoDomainsReady_ReturnsEmpty() {
        // Given: All domains have future retry times
        Domain domain = createDomain("future.com", DomainStatus.PROCESSING_FAILED);
        domain.setRetryAfter(now.plusDays(1));
        domainRepository.save(domain);

        // When: Query for ready domains
        List<Domain> readyToRetry = domainRepository.findDomainsReadyForRetry(now);

        // Then: Empty list
        assertThat(readyToRetry).isEmpty();
    }

    // ===== Test findHighQualityDomains - Quality Filtering =====

    @Test
    void testFindHighQualityDomains_FiltersByMinCandidateCount() {
        // Given: High-quality domain with 5 candidates
        Domain domain1 = createDomain("high-quality.org", DomainStatus.PROCESSED_HIGH_QUALITY);
        domain1.setHighQualityCandidateCount(5);
        domain1.setBestConfidenceScore(new BigDecimal("0.85"));
        domainRepository.save(domain1);

        // Given: High-quality domain with only 2 candidates
        Domain domain2 = createDomain("medium-quality.org", DomainStatus.PROCESSED_HIGH_QUALITY);
        domain2.setHighQualityCandidateCount(2);
        domain2.setBestConfidenceScore(new BigDecimal("0.75"));
        domainRepository.save(domain2);

        // Given: Low-quality domain
        Domain domain3 = createDomain("low-quality.com", DomainStatus.PROCESSED_LOW_QUALITY);
        domain3.setHighQualityCandidateCount(0);
        domainRepository.save(domain3);

        // When: Query for domains with at least 3 high-quality candidates
        List<Domain> highQuality = domainRepository.findHighQualityDomains(3);

        // Then: Only domain1 matches
        assertThat(highQuality).hasSize(1);
        assertThat(highQuality.get(0).getDomainName()).isEqualTo("high-quality.org");
        assertThat(highQuality.get(0).getHighQualityCandidateCount()).isEqualTo(5);
    }

    @Test
    void testFindHighQualityDomains_OrderedByConfidenceScore() {
        // Given: Multiple high-quality domains
        Domain domain1 = createDomain("best.org", DomainStatus.PROCESSED_HIGH_QUALITY);
        domain1.setHighQualityCandidateCount(5);
        domain1.setBestConfidenceScore(new BigDecimal("0.95"));
        domainRepository.save(domain1);

        Domain domain2 = createDomain("good.org", DomainStatus.PROCESSED_HIGH_QUALITY);
        domain2.setHighQualityCandidateCount(5);
        domain2.setBestConfidenceScore(new BigDecimal("0.75"));
        domainRepository.save(domain2);

        Domain domain3 = createDomain("excellent.org", DomainStatus.PROCESSED_HIGH_QUALITY);
        domain3.setHighQualityCandidateCount(5);
        domain3.setBestConfidenceScore(new BigDecimal("0.88"));
        domainRepository.save(domain3);

        // When: Query for high-quality domains
        List<Domain> highQuality = domainRepository.findHighQualityDomains(3);

        // Then: Ordered by confidence score DESC
        assertThat(highQuality).hasSize(3);
        assertThat(highQuality.get(0).getDomainName()).isEqualTo("best.org");
        assertThat(highQuality.get(1).getDomainName()).isEqualTo("excellent.org");
        assertThat(highQuality.get(2).getDomainName()).isEqualTo("good.org");
    }

    // ===== Test findLowQualityDomains - Low-Quality Filtering =====

    @Test
    void testFindLowQualityDomains_FiltersByThreshold() {
        // Given: Domain with 10 low-quality candidates (above threshold)
        Domain domain1 = createDomain("spam1.com", DomainStatus.PROCESSED_LOW_QUALITY);
        domain1.setLowQualityCandidateCount(10);
        domain1.setHighQualityCandidateCount(0);
        domainRepository.save(domain1);

        // Given: Domain with 3 low-quality candidates (below threshold)
        Domain domain2 = createDomain("mixed.org", DomainStatus.PROCESSED_LOW_QUALITY);
        domain2.setLowQualityCandidateCount(3);
        domain2.setHighQualityCandidateCount(1);
        domainRepository.save(domain2);

        // Given: High-quality domain
        Domain domain3 = createDomain("good.org", DomainStatus.PROCESSED_HIGH_QUALITY);
        domain3.setLowQualityCandidateCount(1);
        domain3.setHighQualityCandidateCount(8);
        domainRepository.save(domain3);

        // When: Query for domains with at least 5 low-quality candidates
        List<Domain> lowQuality = domainRepository.findLowQualityDomains(5);

        // Then: Only domain1 matches
        assertThat(lowQuality).hasSize(1);
        assertThat(lowQuality.get(0).getDomainName()).isEqualTo("spam1.com");
        assertThat(lowQuality.get(0).getLowQualityCandidateCount()).isEqualTo(10);
    }

    // ===== Test findNoFundsForYear - Year-Based Re-evaluation =====

    @Test
    void testFindNoFundsForYear_FiltersByYear() {
        // Given: Domain marked "no funds" for 2024
        Domain domain1 = createDomain("no-funds-2024.org", DomainStatus.NO_FUNDS_THIS_YEAR);
        domain1.setNoFundsYear(2024);
        domainRepository.save(domain1);

        // Given: Domain marked "no funds" for 2023
        Domain domain2 = createDomain("no-funds-2023.org", DomainStatus.NO_FUNDS_THIS_YEAR);
        domain2.setNoFundsYear(2023);
        domainRepository.save(domain2);

        // Given: Domain with different status
        Domain domain3 = createDomain("active.org", DomainStatus.PROCESSED_HIGH_QUALITY);
        domainRepository.save(domain3);

        // When: Query for "no funds" domains from 2024
        List<Domain> noFunds2024 = domainRepository.findNoFundsForYear(2024);

        // Then: Only domain1 matches
        assertThat(noFunds2024).hasSize(1);
        assertThat(noFunds2024.get(0).getDomainName()).isEqualTo("no-funds-2024.org");
        assertThat(noFunds2024.get(0).getNoFundsYear()).isEqualTo(2024);
    }

    // ===== Test countByStatus - Status Statistics =====

    @Test
    void testCountByStatus_ReturnsCorrectCount() {
        // Given: Multiple domains with different statuses
        domainRepository.save(createDomain("d1.com", DomainStatus.BLACKLISTED));
        domainRepository.save(createDomain("d2.com", DomainStatus.BLACKLISTED));
        domainRepository.save(createDomain("d3.com", DomainStatus.BLACKLISTED));
        domainRepository.save(createDomain("d4.org", DomainStatus.DISCOVERED));
        domainRepository.save(createDomain("d5.org", DomainStatus.DISCOVERED));

        // When: Count by status
        Long blacklistedCount = domainRepository.countByStatus(DomainStatus.BLACKLISTED);
        Long discoveredCount = domainRepository.countByStatus(DomainStatus.DISCOVERED);
        Long failedCount = domainRepository.countByStatus(DomainStatus.PROCESSING_FAILED);

        // Then: Correct counts
        assertThat(blacklistedCount).isEqualTo(3);
        assertThat(discoveredCount).isEqualTo(2);
        assertThat(failedCount).isEqualTo(0);
    }

    // ===== Helper Methods =====

    private Domain createDomain(String domainName, DomainStatus status) {
        return Domain.builder()
            // Don't set domainId - let PostgreSQL generate it via gen_random_uuid()
            .domainName(domainName)
            .status(status)
            .discoveredAt(now)
            .discoverySessionId(sessionId)
            .processingCount(0)
            .highQualityCandidateCount(0)
            .lowQualityCandidateCount(0)
            .failureCount(0)
            .build();
    }
}
