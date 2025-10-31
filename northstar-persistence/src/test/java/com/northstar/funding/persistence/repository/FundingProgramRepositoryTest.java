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

import com.northstar.funding.domain.Domain;
import com.northstar.funding.domain.DomainStatus;
import com.northstar.funding.domain.FundingProgram;
import com.northstar.funding.domain.Organization;
import com.northstar.funding.domain.ProgramStatus;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for FundingProgramRepository
 *
 * Tests funding program management, URL deduplication, deadline tracking.
 * Uses TestContainers with PostgreSQL for realistic testing.
 *
 * Pattern: @DataJdbcTest - One test class per repository
 * Reference: SearchQueryRepositoryTest.java
 */
@DataJdbcTest
@Testcontainers
@ActiveProfiles("postgres-test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class FundingProgramRepositoryTest {

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
    private FundingProgramRepository repository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private DomainRepository domainRepository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
        organizationRepository.deleteAll();
        domainRepository.deleteAll();
    }

    @Test
    void testSaveAndFindByProgramUrl() {
        // Given
        ensureDomainExists("us-bulgaria.org");
        UUID orgId = createDefaultOrganization("us-bulgaria.org");

        FundingProgram program = FundingProgram.builder()
            .organizationId(orgId)
            .domain("us-bulgaria.org")
            .programName("Education Grant 2025")
            .programUrl("https://us-bulgaria.org/education-grant")
            .description("Annual education grants")
            .status(ProgramStatus.ACTIVE)
            .programConfidence(new BigDecimal("0.90"))
            .isValidFundingOpportunity(true)
            .discoveredAt(LocalDateTime.now())
            .isActive(true)
            .build();

        // When
        repository.save(program);

        // Then
        var found = repository.findByProgramUrl("https://us-bulgaria.org/education-grant");
        assertThat(found).isPresent();
        assertThat(found.get().getProgramName()).isEqualTo("Education Grant 2025");
        assertThat(found.get().getStatus()).isEqualTo(ProgramStatus.ACTIVE);
    }

    @Test
    void testExistsByProgramUrl() {
        // Given
        repository.save(createProgram("Test Program", "https://test.org/program"));

        // When / Then
        assertThat(repository.existsByProgramUrl("https://test.org/program")).isTrue();
        assertThat(repository.existsByProgramUrl("https://other.org/program")).isFalse();
    }

    @Test
    void testFindByOrganizationId() {
        // Given
        ensureDomainExists("test.org");
        ensureDomainExists("other.org");
        UUID orgId = createDefaultOrganization("test.org");
        UUID otherOrgId = createDefaultOrganization("other.org");

        repository.save(createProgram("Program 1", "https://test.org/p1", orgId));
        repository.save(createProgram("Program 2", "https://test.org/p2", orgId));
        repository.save(createProgram("Other", "https://other.org/p1", otherOrgId));

        // When
        var results = repository.findByOrganizationId(orgId);

        // Then
        assertThat(results).hasSize(2);
    }

    @Test
    void testFindByDomain() {
        // Given
        repository.save(createProgram("Program 1", "https://test.org/p1", "test.org"));
        repository.save(createProgram("Program 2", "https://test.org/p2", "test.org"));
        repository.save(createProgram("Other", "https://other.org/p1", "other.org"));

        // When
        var results = repository.findByDomain("test.org");

        // Then
        assertThat(results).hasSize(2);
    }

    @Test
    void testFindByStatus() {
        // Given
        repository.save(createProgram("Active 1", "https://test.org/a1", ProgramStatus.ACTIVE));
        repository.save(createProgram("Active 2", "https://test.org/a2", ProgramStatus.ACTIVE));
        repository.save(createProgram("Expired", "https://test.org/e1", ProgramStatus.EXPIRED));

        // When
        var active = repository.findByStatus(ProgramStatus.ACTIVE);
        var expired = repository.findByStatus(ProgramStatus.EXPIRED);

        // Then
        assertThat(active).hasSize(2);
        assertThat(expired).hasSize(1);
    }

    @Test
    void testFindByIsActive() {
        // Given
        repository.save(createProgram("Active", "https://test.org/active", true));
        repository.save(createProgram("Inactive", "https://test.org/inactive", false));

        // When
        var active = repository.findByIsActive(true);
        var inactive = repository.findByIsActive(false);

        // Then
        assertThat(active).hasSize(1);
        assertThat(inactive).hasSize(1);
    }

    @Test
    void testFindByIsValidFundingOpportunity() {
        // Given
        repository.save(createProgram("Valid", "https://test.org/valid", true, true));
        repository.save(createProgram("Invalid", "https://test.org/invalid", false, true));

        // When
        var valid = repository.findByIsValidFundingOpportunity(true);
        var invalid = repository.findByIsValidFundingOpportunity(false);

        // Then
        assertThat(valid).hasSize(1);
        assertThat(invalid).hasSize(1);
    }

    @Test
    void testFindProgramsWithUpcomingDeadlines() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime upcoming = now.plusDays(7);
        LocalDateTime farFuture = now.plusDays(60);

        ensureDomainExists("test.org");
        UUID orgId = createDefaultOrganization("test.org");

        FundingProgram upcomingProgram = FundingProgram.builder()
            .organizationId(orgId)
            .domain("test.org")
            .programName("Upcoming")
            .programUrl("https://test.org/upcoming")
            .status(ProgramStatus.ACTIVE)
            .applicationDeadline(upcoming)
            .discoveredAt(now)
            .isActive(true)
            .build();

        FundingProgram futureProgram = FundingProgram.builder()
            .organizationId(orgId)
            .domain("test.org")
            .programName("Future")
            .programUrl("https://test.org/future")
            .status(ProgramStatus.ACTIVE)
            .applicationDeadline(farFuture)
            .discoveredAt(now)
            .isActive(true)
            .build();

        repository.save(upcomingProgram);
        repository.save(futureProgram);

        // When
        var results = repository.findProgramsWithUpcomingDeadlines(now, now.plusDays(30));

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getProgramName()).isEqualTo("Upcoming");
    }

    @Test
    void testFindExpiredPrograms() {
        // Given
        LocalDateTime now = LocalDateTime.now();

        ensureDomainExists("test.org");
        UUID orgId = createDefaultOrganization("test.org");

        FundingProgram expired = FundingProgram.builder()
            .organizationId(orgId)
            .domain("test.org")
            .programName("Expired")
            .programUrl("https://test.org/expired")
            .status(ProgramStatus.ACTIVE)
            .applicationDeadline(now.minusDays(1))
            .discoveredAt(now)
            .isActive(true)
            .build();

        FundingProgram active = FundingProgram.builder()
            .organizationId(orgId)
            .domain("test.org")
            .programName("Active")
            .programUrl("https://test.org/active")
            .status(ProgramStatus.ACTIVE)
            .applicationDeadline(now.plusDays(30))
            .discoveredAt(now)
            .isActive(true)
            .build();

        repository.save(expired);
        repository.save(active);

        // When
        var results = repository.findExpiredPrograms(now);

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getProgramName()).isEqualTo("Expired");
    }

    @Test
    void testFindHighConfidencePrograms() {
        // Given
        ensureDomainExists("test.org");
        UUID orgId = createDefaultOrganization("test.org");

        FundingProgram highConf = FundingProgram.builder()
            .organizationId(orgId)
            .domain("test.org")
            .programName("High Confidence")
            .programUrl("https://test.org/high")
            .programConfidence(new BigDecimal("0.95"))
            .isValidFundingOpportunity(true)
            .discoveredAt(LocalDateTime.now())
            .isActive(true)
            .build();

        FundingProgram lowConf = FundingProgram.builder()
            .organizationId(orgId)
            .domain("test.org")
            .programName("Low Confidence")
            .programUrl("https://test.org/low")
            .programConfidence(new BigDecimal("0.50"))
            .isValidFundingOpportunity(true)
            .discoveredAt(LocalDateTime.now())
            .isActive(true)
            .build();

        repository.save(highConf);
        repository.save(lowConf);

        // When
        var results = repository.findHighConfidencePrograms(
            new BigDecimal("0.75"),
            PageRequest.of(0, 10)
        );

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getProgramName()).isEqualTo("High Confidence");
    }

    @Test
    void testFindByIsRecurring() {
        // Given
        ensureDomainExists("test.org");
        UUID orgId = createDefaultOrganization("test.org");

        FundingProgram recurring = FundingProgram.builder()
            .organizationId(orgId)
            .domain("test.org")
            .programName("Annual Grant")
            .programUrl("https://test.org/annual")
            .isRecurring(true)
            .discoveredAt(LocalDateTime.now())
            .isActive(true)
            .build();

        FundingProgram oneTime = FundingProgram.builder()
            .organizationId(orgId)
            .domain("test.org")
            .programName("One Time")
            .programUrl("https://test.org/once")
            .isRecurring(false)
            .discoveredAt(LocalDateTime.now())
            .isActive(true)
            .build();

        repository.save(recurring);
        repository.save(oneTime);

        // When
        var recurringResults = repository.findByIsRecurring(true);
        var oneTimeResults = repository.findByIsRecurring(false);

        // Then
        assertThat(recurringResults).hasSize(1);
        assertThat(oneTimeResults).hasSize(1);
    }

    @Test
    void testSearchByProgramName() {
        // Given
        repository.save(createProgram("Bulgaria Education Grant", "https://test.org/bg-edu"));
        repository.save(createProgram("Bulgarian Tech Fund", "https://test.org/bg-tech"));
        repository.save(createProgram("Greece Innovation", "https://test.org/gr-innov"));

        // When
        var results = repository.searchByProgramName("bulgaria");

        // Then
        assertThat(results).hasSize(2);
    }

    @Test
    void testCountByStatus() {
        // Given
        repository.save(createProgram("Active 1", "https://test.org/a1", ProgramStatus.ACTIVE));
        repository.save(createProgram("Active 2", "https://test.org/a2", ProgramStatus.ACTIVE));
        repository.save(createProgram("Expired", "https://test.org/e1", ProgramStatus.EXPIRED));

        // When
        long activeCount = repository.countByStatus(ProgramStatus.ACTIVE);
        long expiredCount = repository.countByStatus(ProgramStatus.EXPIRED);

        // Then
        assertThat(activeCount).isEqualTo(2);
        assertThat(expiredCount).isEqualTo(1);
    }

    @Test
    void testCountActiveByOrganization() {
        // Given
        ensureDomainExists("test.org");
        UUID orgId = createDefaultOrganization("test.org");

        FundingProgram active1 = FundingProgram.builder()
            .organizationId(orgId)
            .domain("test.org")
            .programName("Active 1")
            .programUrl("https://test.org/a1")
            .status(ProgramStatus.ACTIVE)
            .discoveredAt(LocalDateTime.now())
            .isActive(true)
            .build();

        FundingProgram active2 = FundingProgram.builder()
            .organizationId(orgId)
            .domain("test.org")
            .programName("Active 2")
            .programUrl("https://test.org/a2")
            .status(ProgramStatus.ACTIVE)
            .discoveredAt(LocalDateTime.now())
            .isActive(true)
            .build();

        FundingProgram expired = FundingProgram.builder()
            .organizationId(orgId)
            .domain("test.org")
            .programName("Expired")
            .programUrl("https://test.org/exp")
            .status(ProgramStatus.EXPIRED)
            .discoveredAt(LocalDateTime.now())
            .isActive(true)
            .build();

        repository.save(active1);
        repository.save(active2);
        repository.save(expired);

        // When
        long count = repository.countActiveByOrganization(orgId);

        // Then
        assertThat(count).isEqualTo(2);
    }

    // Helper methods
    private FundingProgram createProgram(String name, String url) {
        return createProgram(name, url, "test.org");
    }

    private FundingProgram createProgram(String name, String url, UUID organizationId) {
        // Extract domain from URL for domain field
        String domainName = "test.org"; // Default
        if (url.contains("other.org")) {
            domainName = "other.org";
        }

        return FundingProgram.builder()
            .organizationId(organizationId)
            .domain(domainName)
            .programName(name)
            .programUrl(url)
            .discoveredAt(LocalDateTime.now())
            .isActive(true)
            .build();
    }

    private FundingProgram createProgram(String name, String url, String domainName) {
        ensureDomainExists(domainName);
        UUID orgId = createDefaultOrganization(domainName);
        return FundingProgram.builder()
            .organizationId(orgId)
            .domain(domainName)
            .programName(name)
            .programUrl(url)
            .discoveredAt(LocalDateTime.now())
            .isActive(true)
            .build();
    }

    private FundingProgram createProgram(String name, String url, ProgramStatus status) {
        ensureDomainExists("test.org");
        UUID orgId = createDefaultOrganization("test.org");
        return FundingProgram.builder()
            .organizationId(orgId)
            .domain("test.org")
            .programName(name)
            .programUrl(url)
            .status(status)
            .discoveredAt(LocalDateTime.now())
            .isActive(true)
            .build();
    }

    private FundingProgram createProgram(String name, String url, boolean isActive) {
        ensureDomainExists("test.org");
        UUID orgId = createDefaultOrganization("test.org");
        return FundingProgram.builder()
            .organizationId(orgId)
            .domain("test.org")
            .programName(name)
            .programUrl(url)
            .isActive(isActive)
            .discoveredAt(LocalDateTime.now())
            .build();
    }

    private FundingProgram createProgram(String name, String url, boolean isValidFundingOpportunity, boolean isActive) {
        ensureDomainExists("test.org");
        UUID orgId = createDefaultOrganization("test.org");
        return FundingProgram.builder()
            .organizationId(orgId)
            .domain("test.org")
            .programName(name)
            .programUrl(url)
            .isValidFundingOpportunity(isValidFundingOpportunity)
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

    private UUID createDefaultOrganization(String domainName) {
        // Check if organization already exists for this domain
        var existing = organizationRepository.findByDomain(domainName);
        if (existing.isPresent()) {
            return existing.get().getOrganizationId();
        }

        Organization org = Organization.builder()
            .name("Test Org for " + domainName)
            .domain(domainName)
            .discoveredAt(LocalDateTime.now())
            .isActive(true)
            .build();
        return organizationRepository.save(org).getOrganizationId();
    }

    private UUID ensureOrganizationExists(UUID preferredId, String domainName) {
        ensureDomainExists(domainName);
        // For tests that pass a specific org ID, create org with that domain
        return createDefaultOrganization(domainName);
    }
}
