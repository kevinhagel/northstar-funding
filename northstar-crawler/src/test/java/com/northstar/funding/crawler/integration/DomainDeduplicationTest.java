package com.northstar.funding.crawler.integration;

import com.northstar.funding.domain.AdminRole;
import com.northstar.funding.domain.AdminUser;
import com.northstar.funding.domain.DiscoverySession;
import com.northstar.funding.domain.Domain;
import com.northstar.funding.domain.DomainStatus;
import com.northstar.funding.domain.SessionStatus;
import com.northstar.funding.domain.SessionType;
import com.northstar.funding.persistence.repository.AdminUserRepository;
import com.northstar.funding.persistence.service.DiscoverySessionService;
import com.northstar.funding.persistence.service.DomainService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration test for Scenario 4: Domain-Level Deduplication.
 *
 * T042: Tests domain deduplication with real database using TestContainers.
 * Verifies that duplicate domains are detected and counted correctly.
 */
@SpringBootTest(classes = TestApplication.class)
@Testcontainers
@Transactional
@DisplayName("Integration Test - Scenario 4: Domain Deduplication")
class DomainDeduplicationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.enabled", () -> "true");
    }

    @Autowired
    private DomainService domainService;

    @Autowired
    private DiscoverySessionService discoverySessionService;

    @Autowired
    private AdminUserRepository adminUserRepository;

    @Test
    @DisplayName("DomainService bean is loaded")
    void contextLoads_DomainService() {
        assertThat(domainService).isNotNull();
    }

    /**
     * Helper method to create a DiscoverySession for tests.
     * Required because Domain has a foreign key to DiscoverySession.
     */
    private UUID createDiscoverySession() {
        DiscoverySession session = DiscoverySession.builder()
                .sessionType(SessionType.MANUAL)
                .status(SessionStatus.RUNNING)
                .build();
        DiscoverySession saved = discoverySessionService.createSession(session);
        return saved.getSessionId();
    }

    @Test
    @DisplayName("New domain can be registered")
    void registerDomain_NewDomain_Success() {
        UUID sessionId = createDiscoverySession();
        Domain domain = domainService.registerDomain("example.org", sessionId);

        assertThat(domain).isNotNull();
        assertThat(domain.getDomainId()).isNotNull();
        assertThat(domain.getDomainName()).isEqualTo("example.org");
        assertThat(domain.getStatus()).isEqualTo(DomainStatus.DISCOVERED);
        assertThat(domain.getDiscoverySessionId()).isEqualTo(sessionId);
    }

    @Test
    @DisplayName("Duplicate domain returns existing record")
    void registerDomain_DuplicateDomain_ReturnsExisting() {
        UUID session1 = createDiscoverySession();
        UUID session2 = createDiscoverySession();

        // First registration
        Domain domain1 = domainService.registerDomain("test.org", session1);
        assertThat(domain1.getDomainId()).isNotNull();

        // Second registration (duplicate) - returns existing domain
        Domain domain2 = domainService.registerDomain("test.org", session2);
        assertThat(domain2.getDomainId()).isEqualTo(domain1.getDomainId()); // Same domain record
    }

    @Test
    @DisplayName("Blacklisted domain can be checked via status")
    void blacklistDomain_WorksCorrectly() {
        UUID sessionId = createDiscoverySession();

        // Create admin user first (foreign key requirement)
        AdminUser adminUser = AdminUser.builder()
                .username("test-admin")
                .fullName("Test Administrator")
                .email("test@example.com")
                .role(AdminRole.REVIEWER)
                .isActive(true)
                .createdAt(java.time.LocalDateTime.now())
                .candidatesReviewed(0)
                .currentWorkload(0)
                .build();
        AdminUser savedAdmin = adminUserRepository.save(adminUser);
        UUID adminUserId = savedAdmin.getUserId();

        // Register and blacklist
        Domain domain = domainService.registerDomain("spam.org", sessionId);
        domainService.blacklistDomain("spam.org", adminUserId, "Spam detected");

        // Verify blacklist status
        Domain updated = domainService.findByDomainName("spam.org").orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(DomainStatus.BLACKLISTED);
        assertThat(updated.getBlacklistReason()).isEqualTo("Spam detected");
        assertThat(updated.getBlacklistedBy()).isEqualTo(adminUserId);
    }

    @Test
    @DisplayName("High quality domains can be tracked via candidate counts")
    void trackHighQualityDomain_WorksCorrectly() {
        UUID sessionId = createDiscoverySession();
        Domain domain = domainService.registerDomain("quality.org", sessionId);

        // Update with high-quality candidates
        domainService.updateCandidateCounts("quality.org", 3, 0, new java.math.BigDecimal("0.85"));

        Domain updated = domainService.findByDomainName("quality.org").orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(DomainStatus.PROCESSED_HIGH_QUALITY);
        assertThat(updated.getHighQualityCandidateCount()).isEqualTo(3);
    }

    @Test
    @DisplayName("Low quality domains can be tracked via candidate counts")
    void trackLowQualityDomain_WorksCorrectly() {
        UUID sessionId = createDiscoverySession();
        Domain domain = domainService.registerDomain("lowquality.org", sessionId);

        // Update with low-quality candidates
        domainService.updateCandidateCounts("lowquality.org", 0, 2, new java.math.BigDecimal("0.40"));

        Domain updated = domainService.findByDomainName("lowquality.org").orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(DomainStatus.PROCESSED_LOW_QUALITY);
        assertThat(updated.getLowQualityCandidateCount()).isEqualTo(2);
    }
}
