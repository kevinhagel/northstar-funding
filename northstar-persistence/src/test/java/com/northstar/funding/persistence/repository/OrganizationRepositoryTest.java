package com.northstar.funding.persistence.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.jdbc.DataJdbcTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.northstar.funding.domain.DiscoverySession;
import com.northstar.funding.domain.Domain;
import com.northstar.funding.domain.DomainStatus;
import com.northstar.funding.domain.Organization;
import com.northstar.funding.domain.SessionStatus;
import com.northstar.funding.domain.SessionType;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for OrganizationRepository
 *
 * Tests organization management, validation tracking, quality filtering.
 * Uses TestContainers with PostgreSQL for realistic testing.
 *
 * Pattern: @DataJdbcTest - One test class per repository
 * Reference: SearchQueryRepositoryTest.java
 */
@DataJdbcTest
@Testcontainers
@ActiveProfiles("postgres-test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class OrganizationRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
        .withDatabaseName("test")
        .withUsername("test")
        .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private OrganizationRepository repository;

    @Autowired
    private DomainRepository domainRepository;

    @Autowired
    private DiscoverySessionRepository discoverySessionRepository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
        domainRepository.deleteAll();
        discoverySessionRepository.deleteAll();
    }

    @Test
    void testSaveAndFindByDomain() {
        // Given
        ensureDomainExists("us-bulgaria.org");

        Organization org = Organization.builder()
            .name("America for Bulgaria Foundation")
            .domain("us-bulgaria.org")
            .mission("Support democratic development in Bulgaria")
            .organizationConfidence(new BigDecimal("0.85"))
            .isValidFundingSource(true)
            .discoveredAt(LocalDateTime.now())
            .isActive(true)
            .programCount(0)
            .build();

        // When
        repository.save(org);

        // Then
        var found = repository.findByDomain("us-bulgaria.org");
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("America for Bulgaria Foundation");
        assertThat(found.get().getIsValidFundingSource()).isTrue();
    }

    @Test
    void testExistsByDomain() {
        // Given
        repository.save(createOrganization("Test Org", "test.org"));

        // When / Then
        assertThat(repository.existsByDomain("test.org")).isTrue();
        assertThat(repository.existsByDomain("nonexistent.org")).isFalse();
    }

    @Test
    void testFindByIsValidFundingSource() {
        // Given
        repository.save(createOrganization("Valid Org 1", "valid1.org", true));
        repository.save(createOrganization("Valid Org 2", "valid2.org", true));
        repository.save(createOrganization("Invalid Org", "invalid.org", false));

        // When
        var validOrgs = repository.findByIsValidFundingSource(true);
        var invalidOrgs = repository.findByIsValidFundingSource(false);

        // Then
        assertThat(validOrgs).hasSize(2);
        assertThat(invalidOrgs).hasSize(1);
    }

    @Test
    void testFindByIsActive() {
        // Given
        repository.save(createOrganization("Active Org", "active.org", true, true));
        repository.save(createOrganization("Inactive Org", "inactive.org", true, false));

        // When
        var activeOrgs = repository.findByIsActive(true);
        var inactiveOrgs = repository.findByIsActive(false);

        // Then
        assertThat(activeOrgs).hasSize(1);
        assertThat(activeOrgs.get(0).getName()).isEqualTo("Active Org");
        assertThat(inactiveOrgs).hasSize(1);
    }

    @Test
    void testFindByDiscoverySessionId() {
        // Given
        // Create actual discovery sessions
        DiscoverySession session1 = DiscoverySession.builder()
            .sessionType(SessionType.SCHEDULED)
            .status(SessionStatus.RUNNING)
            .startedAt(LocalDateTime.now())
            .build();
        UUID sessionId = discoverySessionRepository.save(session1).getSessionId();

        DiscoverySession session2 = DiscoverySession.builder()
            .sessionType(SessionType.MANUAL)
            .status(SessionStatus.RUNNING)
            .startedAt(LocalDateTime.now())
            .build();
        UUID otherSessionId = discoverySessionRepository.save(session2).getSessionId();

        ensureDomainExists("org1.org");
        Organization org1 = Organization.builder()

            .name("Org 1")
            .domain("org1.org")
            .discoverySessionId(sessionId)
            .discoveredAt(LocalDateTime.now())
            .isActive(true)
            .build();

        ensureDomainExists("org2.org");
        Organization org2 = Organization.builder()

            .name("Org 2")
            .domain("org2.org")
            .discoverySessionId(sessionId)
            .discoveredAt(LocalDateTime.now())
            .isActive(true)
            .build();

        ensureDomainExists("other.org");
        Organization otherSession = Organization.builder()

            .name("Other")
            .domain("other.org")
            .discoverySessionId(otherSessionId)
            .discoveredAt(LocalDateTime.now())
            .isActive(true)
            .build();

        repository.save(org1);
        repository.save(org2);
        repository.save(otherSession);

        // When
        var results = repository.findByDiscoverySessionId(sessionId);

        // Then
        assertThat(results).hasSize(2);
    }

    @Test
    void testFindByDiscoveredAtAfter() {
        // Given
        LocalDateTime cutoff = LocalDateTime.now().minusDays(1);

        ensureDomainExists("recent.org");
        Organization recent = Organization.builder()

            .name("Recent")
            .domain("recent.org")
            .discoveredAt(LocalDateTime.now())
            .isActive(true)
            .build();

        ensureDomainExists("old.org");
        Organization old = Organization.builder()

            .name("Old")
            .domain("old.org")
            .discoveredAt(cutoff.minusDays(1))
            .isActive(true)
            .build();

        repository.save(recent);
        repository.save(old);

        // When
        var results = repository.findByDiscoveredAtAfter(cutoff);

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getName()).isEqualTo("Recent");
    }

    @Test
    void testFindHighConfidenceOrganizations() {
        // Given
        ensureDomainExists("high1.org");
        Organization highConf1 = Organization.builder()

            .name("High Conf 1")
            .domain("high1.org")
            .organizationConfidence(new BigDecimal("0.95"))
            .isValidFundingSource(true)
            .discoveredAt(LocalDateTime.now())
            .isActive(true)
            .build();

        ensureDomainExists("high2.org");
        Organization highConf2 = Organization.builder()

            .name("High Conf 2")
            .domain("high2.org")
            .organizationConfidence(new BigDecimal("0.85"))
            .isValidFundingSource(true)
            .discoveredAt(LocalDateTime.now())
            .isActive(true)
            .build();

        ensureDomainExists("low.org");
        Organization lowConf = Organization.builder()

            .name("Low Conf")
            .domain("low.org")
            .organizationConfidence(new BigDecimal("0.50"))
            .isValidFundingSource(true)
            .discoveredAt(LocalDateTime.now())
            .isActive(true)
            .build();

        repository.save(highConf1);
        repository.save(highConf2);
        repository.save(lowConf);

        // When
        var results = repository.findHighConfidenceOrganizations(
            new BigDecimal("0.75"),
            PageRequest.of(0, 10)
        );

        // Then
        assertThat(results).hasSize(2);
        assertThat(results.get(0).getName()).isEqualTo("High Conf 1"); // Highest first
    }

    @Test
    void testFindOrganizationsWithMultiplePrograms() {
        // Given
        ensureDomainExists("many.org");
        Organization manyPrograms = Organization.builder()

            .name("Many Programs")
            .domain("many.org")
            .programCount(5)
            .discoveredAt(LocalDateTime.now())
            .isActive(true)
            .build();

        ensureDomainExists("few.org");
        Organization fewPrograms = Organization.builder()

            .name("Few Programs")
            .domain("few.org")
            .programCount(1)
            .discoveredAt(LocalDateTime.now())
            .isActive(true)
            .build();

        repository.save(manyPrograms);
        repository.save(fewPrograms);

        // When
        var results = repository.findOrganizationsWithMultiplePrograms(3);

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getName()).isEqualTo("Many Programs");
    }

    @Test
    void testSearchByName() {
        // Given
        repository.save(createOrganization("Bulgaria Foundation", "bg1.org"));
        repository.save(createOrganization("Bulgarian Tech Hub", "bg2.org"));
        repository.save(createOrganization("Greece Innovation", "gr.org"));

        // When
        var results = repository.searchByName("bulgaria");

        // Then
        assertThat(results).hasSize(2);
        assertThat(results)
            .extracting(Organization::getName)
            .containsExactlyInAnyOrder("Bulgaria Foundation", "Bulgarian Tech Hub");
    }

    @Test
    void testCountByValidationStatus() {
        // Given
        repository.save(createOrganization("Valid 1", "valid1.org", true));
        repository.save(createOrganization("Valid 2", "valid2.org", true));
        repository.save(createOrganization("Invalid", "invalid.org", false));

        // When
        long validCount = repository.countByValidationStatus(true);
        long invalidCount = repository.countByValidationStatus(false);

        // Then
        assertThat(validCount).isEqualTo(2);
        assertThat(invalidCount).isEqualTo(1);
    }

    @Test
    void testFindOrganizationsNeedingRefresh() {
        // Given
        LocalDateTime threshold = LocalDateTime.now().minusDays(30);

        ensureDomainExists("stale.org");
        Organization needsRefresh = Organization.builder()

            .name("Needs Refresh")
            .domain("stale.org")
            .lastRefreshedAt(threshold.minusDays(1))
            .discoveredAt(LocalDateTime.now())
            .isActive(true)
            .build();

        ensureDomainExists("fresh.org");
        Organization fresh = Organization.builder()

            .name("Fresh")
            .domain("fresh.org")
            .lastRefreshedAt(LocalDateTime.now())
            .discoveredAt(LocalDateTime.now())
            .isActive(true)
            .build();

        ensureDomainExists("never.org");
        Organization neverRefreshed = Organization.builder()

            .name("Never Refreshed")
            .domain("never.org")
            .lastRefreshedAt(null)
            .discoveredAt(LocalDateTime.now())
            .isActive(true)
            .build();

        repository.save(needsRefresh);
        repository.save(fresh);
        repository.save(neverRefreshed);

        // When
        var results = repository.findOrganizationsNeedingRefresh(threshold);

        // Then
        assertThat(results).hasSize(2); // neverRefreshed and needsRefresh
        assertThat(results.get(0).getName()).isEqualTo("Never Refreshed"); // NULLS FIRST
    }

    // Helper methods
    private Organization createOrganization(String name, String domainName) {
        ensureDomainExists(domainName);
        return Organization.builder()
            .name(name)
            .domain(domainName)
            .discoveredAt(LocalDateTime.now())
            .isActive(true)
            .build();
    }

    private Organization createOrganization(String name, String domainName, boolean isValidFundingSource) {
        ensureDomainExists(domainName);
        return Organization.builder()
            .name(name)
            .domain(domainName)
            .isValidFundingSource(isValidFundingSource)
            .discoveredAt(LocalDateTime.now())
            .isActive(true)
            .build();
    }

    private Organization createOrganization(String name, String domainName, boolean isValidFundingSource, boolean isActive) {
        ensureDomainExists(domainName);
        return Organization.builder()
            .name(name)
            .domain(domainName)
            .isValidFundingSource(isValidFundingSource)
            .isActive(isActive)
            .discoveredAt(LocalDateTime.now())
            .build();
    }

    private void ensureDomainExists(String domainName) {
        if (!domainRepository.existsByDomainName(domainName)) {
            domainRepository.save(Domain.builder()
                .domainName(domainName)
                .status(DomainStatus.DISCOVERED)
                .discoveredAt(LocalDateTime.now())
                .build());
        }
    }
}
