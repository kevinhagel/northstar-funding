package com.northstar.funding.discovery.service;

import com.northstar.funding.discovery.domain.Domain;
import com.northstar.funding.discovery.domain.DomainStatus;
import com.northstar.funding.discovery.infrastructure.DomainRepository;
import io.vavr.control.Try;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for DomainRegistryService
 *
 * Tests domain-level deduplication, blacklist management, and quality tracking.
 * Uses Mockito to mock DomainRepository.
 *
 * Critical TDD Flow:
 * 1. Write this test FIRST (T003)
 * 2. Tests should PASS (service already implemented)
 * 3. Verify all domain registry business logic
 */
@ExtendWith(MockitoExtension.class)
class DomainRegistryServiceTest {

    @Mock
    private DomainRepository domainRepository;

    @InjectMocks
    private DomainRegistryService domainRegistryService;

    @Captor
    private ArgumentCaptor<Domain> domainCaptor;

    private UUID sessionId;
    private UUID adminUserId;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        sessionId = UUID.randomUUID();
        adminUserId = UUID.randomUUID();
        now = LocalDateTime.now();
    }

    // ===== Test extractDomainName - Domain Extraction =====

    @Test
    void testExtractDomainName_StandardUrl_ExtractsDomain() {
        // When: Extract domain from standard URL
        Try<String> result = domainRegistryService.extractDomainName("https://us-bulgaria.org/programs/education");

        // Then: Domain extracted correctly
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.get()).isEqualTo("us-bulgaria.org");
    }

    @Test
    void testExtractDomainName_WithWww_RemovesWwwPrefix() {
        // When: Extract domain with www prefix
        Try<String> result = domainRegistryService.extractDomainName("https://www.example.org/about");

        // Then: www. prefix removed
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.get()).isEqualTo("example.org");
    }

    @Test
    void testExtractDomainName_Uppercase_ConvertsToLowercase() {
        // When: Extract domain with uppercase letters
        Try<String> result = domainRegistryService.extractDomainName("https://EXAMPLE.ORG/About");

        // Then: Converted to lowercase
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.get()).isEqualTo("example.org");
    }

    @Test
    void testExtractDomainName_HttpUrl_ExtractsDomain() {
        // When: Extract domain from http (not https)
        Try<String> result = domainRegistryService.extractDomainName("http://example.org");

        // Then: Domain extracted
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.get()).isEqualTo("example.org");
    }

    @Test
    void testExtractDomainName_WithPort_ExtractsDomainWithoutPort() {
        // When: Extract domain from URL with port
        Try<String> result = domainRegistryService.extractDomainName("https://localhost:8080/test");

        // Then: Domain extracted without port
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.get()).isEqualTo("localhost");
    }

    @Test
    void testExtractDomainName_InvalidUrl_ReturnsFailure() {
        // When: Extract domain from invalid URL
        Try<String> result = domainRegistryService.extractDomainName("not-a-valid-url");

        // Then: Returns failure
        assertThat(result.isFailure()).isTrue();
    }

    @Test
    void testExtractDomainName_UrlWithoutHost_ReturnsFailure() {
        // When: Extract domain from URL without host
        Try<String> result = domainRegistryService.extractDomainName("file:///path/to/file");

        // Then: Returns failure (no host in file:// URLs)
        assertThat(result.isFailure()).isTrue();
    }

    // ===== Test shouldProcessDomainByName - Processing Decision =====

    @Test
    void testShouldProcessDomainByName_NewDomain_ReturnsTrue() {
        // Given: Domain does not exist
        when(domainRepository.findByDomainName("new-domain.org")).thenReturn(Optional.empty());

        // When: Check if should process
        boolean shouldProcess = domainRegistryService.shouldProcessDomainByName("new-domain.org");

        // Then: Should process (new domain)
        assertThat(shouldProcess).isTrue();
    }

    @Test
    void testShouldProcessDomainByName_BlacklistedDomain_ReturnsFalse() {
        // Given: Domain is blacklisted
        Domain blacklisted = createDomain("blacklisted.com", DomainStatus.BLACKLISTED);
        blacklisted.setBlacklistReason("Known scam site");
        when(domainRepository.findByDomainName("blacklisted.com")).thenReturn(Optional.of(blacklisted));

        // When: Check if should process
        boolean shouldProcess = domainRegistryService.shouldProcessDomainByName("blacklisted.com");

        // Then: Should NOT process (blacklisted)
        assertThat(shouldProcess).isFalse();
    }

    @Test
    void testShouldProcessDomainByName_LowQualityDomain_ReturnsFalse() {
        // Given: Domain marked as low quality
        Domain lowQuality = createDomain("low-quality.com", DomainStatus.PROCESSED_LOW_QUALITY);
        lowQuality.setLowQualityCandidateCount(5);
        lowQuality.setHighQualityCandidateCount(0);
        when(domainRepository.findByDomainName("low-quality.com")).thenReturn(Optional.of(lowQuality));

        // When: Check if should process
        boolean shouldProcess = domainRegistryService.shouldProcessDomainByName("low-quality.com");

        // Then: Should NOT process (low quality)
        assertThat(shouldProcess).isFalse();
    }

    @Test
    void testShouldProcessDomainByName_NoFundsThisYear_CurrentYear_ReturnsFalse() {
        // Given: Domain marked "no funds this year" for current year
        int currentYear = LocalDateTime.now().getYear();
        Domain noFunds = createDomain("no-funds.org", DomainStatus.NO_FUNDS_THIS_YEAR);
        noFunds.setNoFundsYear(currentYear);
        when(domainRepository.findByDomainName("no-funds.org")).thenReturn(Optional.of(noFunds));

        // When: Check if should process
        boolean shouldProcess = domainRegistryService.shouldProcessDomainByName("no-funds.org");

        // Then: Should NOT process (no funds for current year)
        assertThat(shouldProcess).isFalse();
    }

    @Test
    void testShouldProcessDomainByName_NoFundsThisYear_PreviousYear_ReturnsTrue() {
        // Given: Domain marked "no funds this year" for PREVIOUS year
        int previousYear = LocalDateTime.now().getYear() - 1;
        Domain noFunds = createDomain("no-funds-2023.org", DomainStatus.NO_FUNDS_THIS_YEAR);
        noFunds.setNoFundsYear(previousYear);
        when(domainRepository.findByDomainName("no-funds-2023.org")).thenReturn(Optional.of(noFunds));

        // When: Check if should process
        boolean shouldProcess = domainRegistryService.shouldProcessDomainByName("no-funds-2023.org");

        // Then: Should process (new year, re-check allowed)
        assertThat(shouldProcess).isTrue();
    }

    @Test
    void testShouldProcessDomainByName_ProcessingFailed_RetryNotReady_ReturnsFalse() {
        // Given: Domain failed processing with future retry time
        Domain failed = createDomain("failed.com", DomainStatus.PROCESSING_FAILED);
        failed.setRetryAfter(now.plusHours(2)); // Retry in 2 hours
        failed.setFailureCount(2);
        when(domainRepository.findByDomainName("failed.com")).thenReturn(Optional.of(failed));

        // When: Check if should process
        boolean shouldProcess = domainRegistryService.shouldProcessDomainByName("failed.com");

        // Then: Should NOT process (retry time not reached)
        assertThat(shouldProcess).isFalse();
    }

    @Test
    void testShouldProcessDomainByName_ProcessingFailed_RetryReady_ReturnsTrue() {
        // Given: Domain failed processing with past retry time
        Domain failed = createDomain("failed-ready.com", DomainStatus.PROCESSING_FAILED);
        failed.setRetryAfter(now.minusHours(1)); // Retry time passed
        failed.setFailureCount(1);
        when(domainRepository.findByDomainName("failed-ready.com")).thenReturn(Optional.of(failed));

        // When: Check if should process
        boolean shouldProcess = domainRegistryService.shouldProcessDomainByName("failed-ready.com");

        // Then: Should process (retry time passed)
        assertThat(shouldProcess).isTrue();
    }

    @Test
    void testShouldProcessDomainByName_DiscoveredStatus_ReturnsTrue() {
        // Given: Domain with DISCOVERED status
        Domain discovered = createDomain("new.org", DomainStatus.DISCOVERED);
        when(domainRepository.findByDomainName("new.org")).thenReturn(Optional.of(discovered));

        // When: Check if should process
        boolean shouldProcess = domainRegistryService.shouldProcessDomainByName("new.org");

        // Then: Should process
        assertThat(shouldProcess).isTrue();
    }

    @Test
    void testShouldProcessDomainByName_ProcessingStatus_ReturnsTrue() {
        // Given: Domain with PROCESSING status
        Domain processing = createDomain("processing.org", DomainStatus.PROCESSING);
        when(domainRepository.findByDomainName("processing.org")).thenReturn(Optional.of(processing));

        // When: Check if should process
        boolean shouldProcess = domainRegistryService.shouldProcessDomainByName("processing.org");

        // Then: Should process (allow concurrent processing)
        assertThat(shouldProcess).isTrue();
    }

    @Test
    void testShouldProcessDomainByName_HighQualityStatus_ReturnsTrue() {
        // Given: Domain with PROCESSED_HIGH_QUALITY status
        Domain highQuality = createDomain("good.org", DomainStatus.PROCESSED_HIGH_QUALITY);
        highQuality.setHighQualityCandidateCount(5);
        highQuality.setBestConfidenceScore(new BigDecimal("0.85"));
        when(domainRepository.findByDomainName("good.org")).thenReturn(Optional.of(highQuality));

        // When: Check if should process
        boolean shouldProcess = domainRegistryService.shouldProcessDomainByName("good.org");

        // Then: Should process (high-quality domains can be re-checked periodically)
        assertThat(shouldProcess).isTrue();
    }

    // ===== Test shouldProcessDomain - URL-Based Processing Decision =====

    @Test
    void testShouldProcessDomain_ValidUrl_BlacklistedDomain_ReturnsFalse() {
        // Given: URL with blacklisted domain
        Domain blacklisted = createDomain("spam.com", DomainStatus.BLACKLISTED);
        when(domainRepository.findByDomainName("spam.com")).thenReturn(Optional.of(blacklisted));

        // When: Check if should process URL
        boolean shouldProcess = domainRegistryService.shouldProcessDomain("https://spam.com/page");

        // Then: Should NOT process
        assertThat(shouldProcess).isFalse();
    }

    @Test
    void testShouldProcessDomain_InvalidUrl_ReturnsTrue() {
        // When: Check if should process invalid URL
        boolean shouldProcess = domainRegistryService.shouldProcessDomain("not-a-url");

        // Then: Returns true (allow processing of unusual URLs, may be edge case)
        assertThat(shouldProcess).isTrue();
        verify(domainRepository, never()).findByDomainName(anyString());
    }

    // ===== Test registerDomain - Domain Registration =====

    @Test
    void testRegisterDomain_NewDomain_CreatesDomain() {
        // Given: Domain does not exist
        when(domainRepository.findByDomainName("new-domain.org")).thenReturn(Optional.empty());
        when(domainRepository.save(any(Domain.class))).thenAnswer(invocation -> {
            Domain domain = invocation.getArgument(0);
            domain.setDomainId(UUID.randomUUID()); // Simulate DB-generated ID
            return domain;
        });

        // When: Register new domain
        Try<Domain> result = domainRegistryService.registerDomain("new-domain.org", sessionId);

        // Then: Domain created
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.get().getDomainName()).isEqualTo("new-domain.org");
        assertThat(result.get().getStatus()).isEqualTo(DomainStatus.DISCOVERED);
        assertThat(result.get().getDiscoverySessionId()).isEqualTo(sessionId);
        assertThat(result.get().getProcessingCount()).isEqualTo(0);
        assertThat(result.get().getHighQualityCandidateCount()).isEqualTo(0);
        assertThat(result.get().getLowQualityCandidateCount()).isEqualTo(0);
        assertThat(result.get().getFailureCount()).isEqualTo(0);

        verify(domainRepository).save(domainCaptor.capture());
        Domain saved = domainCaptor.getValue();
        assertThat(saved.getDomainName()).isEqualTo("new-domain.org");
        assertThat(saved.getStatus()).isEqualTo(DomainStatus.DISCOVERED);
    }

    @Test
    void testRegisterDomain_ExistingDomain_ReturnsExisting() {
        // Given: Domain already exists
        Domain existing = createDomain("existing.org", DomainStatus.DISCOVERED);
        existing.setDomainId(UUID.randomUUID());
        when(domainRepository.findByDomainName("existing.org")).thenReturn(Optional.of(existing));

        // When: Register existing domain
        Try<Domain> result = domainRegistryService.registerDomain("existing.org", sessionId);

        // Then: Returns existing domain (no new domain created)
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.get()).isEqualTo(existing);
        verify(domainRepository, never()).save(any());
    }

    @Test
    void testRegisterDomainFromUrl_ValidUrl_RegistersDomain() {
        // Given: Valid URL with new domain
        when(domainRepository.findByDomainName("example.org")).thenReturn(Optional.empty());
        when(domainRepository.save(any(Domain.class))).thenAnswer(invocation -> {
            Domain domain = invocation.getArgument(0);
            domain.setDomainId(UUID.randomUUID());
            return domain;
        });

        // When: Register domain from URL
        Try<Domain> result = domainRegistryService.registerDomainFromUrl(
            "https://example.org/programs", sessionId);

        // Then: Domain registered
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.get().getDomainName()).isEqualTo("example.org");
    }

    @Test
    void testRegisterDomainFromUrl_InvalidUrl_ReturnsFailure() {
        // When: Register domain from invalid URL
        Try<Domain> result = domainRegistryService.registerDomainFromUrl(
            "not-a-url", sessionId);

        // Then: Returns failure
        assertThat(result.isFailure()).isTrue();
        verify(domainRepository, never()).findByDomainName(anyString());
    }

    // ===== Test updateDomainQuality - Quality Metrics =====

    @Test
    void testUpdateDomainQuality_HighConfidence_UpdatesBestScore() {
        // Given: Domain with lower confidence score
        UUID domainId = UUID.randomUUID();
        Domain domain = createDomain("example.org", DomainStatus.DISCOVERED);
        domain.setDomainId(domainId);
        domain.setBestConfidenceScore(new BigDecimal("0.50"));
        when(domainRepository.findById(domainId)).thenReturn(Optional.of(domain));

        // When: Update with higher confidence score
        domainRegistryService.updateDomainQuality(domainId, new BigDecimal("0.85"), true);

        // Then: Best score updated
        verify(domainRepository).save(domainCaptor.capture());
        Domain saved = domainCaptor.getValue();
        assertThat(saved.getBestConfidenceScore()).isEqualByComparingTo(new BigDecimal("0.85"));
        assertThat(saved.getHighQualityCandidateCount()).isEqualTo(1);
        assertThat(saved.getStatus()).isEqualTo(DomainStatus.PROCESSED_HIGH_QUALITY);
        assertThat(saved.getProcessingCount()).isEqualTo(1);
        assertThat(saved.getLastProcessedAt()).isNotNull();
    }

    @Test
    void testUpdateDomainQuality_LowerConfidence_DoesNotUpdateBestScore() {
        // Given: Domain with higher confidence score
        UUID domainId = UUID.randomUUID();
        Domain domain = createDomain("example.org", DomainStatus.PROCESSED_HIGH_QUALITY);
        domain.setDomainId(domainId);
        domain.setBestConfidenceScore(new BigDecimal("0.90"));
        domain.setHighQualityCandidateCount(2);
        when(domainRepository.findById(domainId)).thenReturn(Optional.of(domain));

        // When: Update with lower confidence score
        domainRegistryService.updateDomainQuality(domainId, new BigDecimal("0.65"), true);

        // Then: Best score NOT updated (kept at 0.9)
        verify(domainRepository).save(domainCaptor.capture());
        Domain saved = domainCaptor.getValue();
        assertThat(saved.getBestConfidenceScore()).isEqualByComparingTo(new BigDecimal("0.90")); // Unchanged
        assertThat(saved.getHighQualityCandidateCount()).isEqualTo(3); // Incremented
    }

    @Test
    void testUpdateDomainQuality_HighQuality_IncrementsHighQualityCount() {
        // Given: Domain with some quality data
        UUID domainId = UUID.randomUUID();
        Domain domain = createDomain("good.org", DomainStatus.DISCOVERED);
        domain.setDomainId(domainId);
        domain.setHighQualityCandidateCount(3);
        domain.setLowQualityCandidateCount(1);
        when(domainRepository.findById(domainId)).thenReturn(Optional.of(domain));

        // When: Update with high quality candidate
        domainRegistryService.updateDomainQuality(domainId, new BigDecimal("0.75"), true);

        // Then: High quality count incremented, low quality unchanged
        verify(domainRepository).save(domainCaptor.capture());
        Domain saved = domainCaptor.getValue();
        assertThat(saved.getHighQualityCandidateCount()).isEqualTo(4);
        assertThat(saved.getLowQualityCandidateCount()).isEqualTo(1); // Unchanged
        assertThat(saved.getStatus()).isEqualTo(DomainStatus.PROCESSED_HIGH_QUALITY);
    }

    @Test
    void testUpdateDomainQuality_LowQuality_IncrementsLowQualityCount() {
        // Given: Domain with some quality data
        UUID domainId = UUID.randomUUID();
        Domain domain = createDomain("mixed.org", DomainStatus.DISCOVERED);
        domain.setDomainId(domainId);
        domain.setHighQualityCandidateCount(1);
        domain.setLowQualityCandidateCount(1);
        when(domainRepository.findById(domainId)).thenReturn(Optional.of(domain));

        // When: Update with low quality candidate
        domainRegistryService.updateDomainQuality(domainId, new BigDecimal("0.30"), false);

        // Then: Low quality count incremented, high quality unchanged
        verify(domainRepository).save(domainCaptor.capture());
        Domain saved = domainCaptor.getValue();
        assertThat(saved.getHighQualityCandidateCount()).isEqualTo(1); // Unchanged
        assertThat(saved.getLowQualityCandidateCount()).isEqualTo(2);
        assertThat(saved.getStatus()).isEqualTo(DomainStatus.DISCOVERED); // Not marked low quality yet (needs 3+)
    }

    @Test
    void testUpdateDomainQuality_ThreeLowQualityWithNoHighQuality_MarksAsLowQuality() {
        // Given: Domain with 2 low-quality candidates and 0 high-quality
        UUID domainId = UUID.randomUUID();
        Domain domain = createDomain("spam.com", DomainStatus.DISCOVERED);
        domain.setDomainId(domainId);
        domain.setHighQualityCandidateCount(0);
        domain.setLowQualityCandidateCount(2);
        when(domainRepository.findById(domainId)).thenReturn(Optional.of(domain));

        // When: Add 3rd low-quality candidate (triggers threshold)
        domainRegistryService.updateDomainQuality(domainId, new BigDecimal("0.20"), false);

        // Then: Domain marked as PROCESSED_LOW_QUALITY
        verify(domainRepository).save(domainCaptor.capture());
        Domain saved = domainCaptor.getValue();
        assertThat(saved.getLowQualityCandidateCount()).isEqualTo(3);
        assertThat(saved.getStatus()).isEqualTo(DomainStatus.PROCESSED_LOW_QUALITY);
    }

    @Test
    void testUpdateDomainQuality_ThreeLowQualityWithHighQuality_DoesNotMarkAsLowQuality() {
        // Given: Domain with 2 low-quality candidates and 1 high-quality
        UUID domainId = UUID.randomUUID();
        Domain domain = createDomain("mixed.org", DomainStatus.DISCOVERED);
        domain.setDomainId(domainId);
        domain.setHighQualityCandidateCount(1); // Has high-quality candidates
        domain.setLowQualityCandidateCount(2);
        when(domainRepository.findById(domainId)).thenReturn(Optional.of(domain));

        // When: Add 3rd low-quality candidate
        domainRegistryService.updateDomainQuality(domainId, new BigDecimal("0.25"), false);

        // Then: NOT marked as low quality (has at least one high-quality candidate)
        verify(domainRepository).save(domainCaptor.capture());
        Domain saved = domainCaptor.getValue();
        assertThat(saved.getLowQualityCandidateCount()).isEqualTo(3);
        assertThat(saved.getStatus()).isEqualTo(DomainStatus.DISCOVERED); // NOT marked low quality
    }

    @Test
    void testUpdateDomainQuality_NonExistentDomain_DoesNothing() {
        // Given: Domain does not exist
        UUID domainId = UUID.randomUUID();
        when(domainRepository.findById(domainId)).thenReturn(Optional.empty());

        // When: Update quality
        domainRegistryService.updateDomainQuality(domainId, new BigDecimal("0.75"), true);

        // Then: No save operation
        verify(domainRepository, never()).save(any());
    }

    // ===== Test blacklistDomain - Blacklist Management =====

    @Test
    void testBlacklistDomain_NewDomain_CreatesAndBlacklists() {
        // Given: Domain does not exist
        when(domainRepository.findByDomainName("scam.com")).thenReturn(Optional.empty());
        when(domainRepository.save(any(Domain.class))).thenAnswer(invocation -> {
            Domain domain = invocation.getArgument(0);
            domain.setDomainId(UUID.randomUUID());
            return domain;
        });

        // When: Blacklist new domain
        Try<Domain> result = domainRegistryService.blacklistDomain(
            "scam.com", "Known scam site", adminUserId);

        // Then: Domain created and blacklisted
        assertThat(result.isSuccess()).isTrue();
        verify(domainRepository).save(domainCaptor.capture());
        Domain saved = domainCaptor.getValue();
        assertThat(saved.getDomainName()).isEqualTo("scam.com");
        assertThat(saved.getStatus()).isEqualTo(DomainStatus.BLACKLISTED);
        assertThat(saved.getBlacklistReason()).isEqualTo("Known scam site");
        assertThat(saved.getBlacklistedBy()).isEqualTo(adminUserId);
        assertThat(saved.getBlacklistedAt()).isNotNull();
    }

    @Test
    void testBlacklistDomain_ExistingDomain_UpdatesStatus() {
        // Given: Domain exists with different status
        Domain existing = createDomain("spam.org", DomainStatus.DISCOVERED);
        existing.setDomainId(UUID.randomUUID());
        existing.setHighQualityCandidateCount(2);
        when(domainRepository.findByDomainName("spam.org")).thenReturn(Optional.of(existing));
        when(domainRepository.save(any(Domain.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When: Blacklist existing domain
        Try<Domain> result = domainRegistryService.blacklistDomain(
            "spam.org", "Spam aggregator site", adminUserId);

        // Then: Status updated to BLACKLISTED
        assertThat(result.isSuccess()).isTrue();
        verify(domainRepository).save(domainCaptor.capture());
        Domain saved = domainCaptor.getValue();
        assertThat(saved.getStatus()).isEqualTo(DomainStatus.BLACKLISTED);
        assertThat(saved.getBlacklistReason()).isEqualTo("Spam aggregator site");
        assertThat(saved.getBlacklistedBy()).isEqualTo(adminUserId);
        assertThat(saved.getBlacklistedAt()).isNotNull();
        // Existing data preserved
        assertThat(saved.getHighQualityCandidateCount()).isEqualTo(2);
    }

    // ===== Test markNoFundsThisYear - "No Funds" Management =====

    @Test
    void testMarkNoFundsThisYear_ExistingDomain_UpdatesStatus() {
        // Given: Domain exists
        Domain existing = createDomain("funder.org", DomainStatus.DISCOVERED);
        when(domainRepository.findByDomainName("funder.org")).thenReturn(Optional.of(existing));
        when(domainRepository.save(any(Domain.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When: Mark as no funds for 2025
        Try<Domain> result = domainRegistryService.markNoFundsThisYear(
            "funder.org", 2025, "No funding rounds planned for 2025");

        // Then: Status updated
        assertThat(result.isSuccess()).isTrue();
        verify(domainRepository).save(domainCaptor.capture());
        Domain saved = domainCaptor.getValue();
        assertThat(saved.getStatus()).isEqualTo(DomainStatus.NO_FUNDS_THIS_YEAR);
        assertThat(saved.getNoFundsYear()).isEqualTo(2025);
        assertThat(saved.getNotes()).isEqualTo("No funding rounds planned for 2025");
    }

    @Test
    void testMarkNoFundsThisYear_NonExistentDomain_ReturnsFailure() {
        // Given: Domain does not exist
        when(domainRepository.findByDomainName("nonexistent.org")).thenReturn(Optional.empty());

        // When: Mark as no funds
        Try<Domain> result = domainRegistryService.markNoFundsThisYear(
            "nonexistent.org", 2025, "No funds");

        // Then: Returns failure (domain must exist first)
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getCause()).isInstanceOf(IllegalArgumentException.class);
        verify(domainRepository, never()).save(any());
    }

    // ===== Test recordProcessingFailure - Exponential Backoff =====

    @Test
    void testRecordProcessingFailure_FirstFailure_RetryIn1Hour() {
        // Given: Domain with 0 failures
        UUID domainId = UUID.randomUUID();
        Domain domain = createDomain("flaky.com", DomainStatus.DISCOVERED);
        domain.setDomainId(domainId);
        domain.setFailureCount(0);
        when(domainRepository.findById(domainId)).thenReturn(Optional.of(domain));

        // When: Record first failure
        domainRegistryService.recordProcessingFailure(domainId, "Connection timeout");

        // Then: Retry in 1 hour
        verify(domainRepository).save(domainCaptor.capture());
        Domain saved = domainCaptor.getValue();
        assertThat(saved.getFailureCount()).isEqualTo(1);
        assertThat(saved.getFailureReason()).isEqualTo("Connection timeout");
        assertThat(saved.getStatus()).isEqualTo(DomainStatus.PROCESSING_FAILED);
        assertThat(saved.getRetryAfter()).isBetween(
            now.plusHours(1).minusSeconds(5),
            now.plusHours(1).plusSeconds(5)
        );
    }

    @Test
    void testRecordProcessingFailure_SecondFailure_RetryIn4Hours() {
        // Given: Domain with 1 failure
        UUID domainId = UUID.randomUUID();
        Domain domain = createDomain("flaky.com", DomainStatus.PROCESSING_FAILED);
        domain.setDomainId(domainId);
        domain.setFailureCount(1);
        when(domainRepository.findById(domainId)).thenReturn(Optional.of(domain));

        // When: Record second failure
        domainRegistryService.recordProcessingFailure(domainId, "500 Internal Server Error");

        // Then: Retry in 4 hours
        verify(domainRepository).save(domainCaptor.capture());
        Domain saved = domainCaptor.getValue();
        assertThat(saved.getFailureCount()).isEqualTo(2);
        assertThat(saved.getRetryAfter()).isBetween(
            now.plusHours(4).minusSeconds(5),
            now.plusHours(4).plusSeconds(5)
        );
    }

    @Test
    void testRecordProcessingFailure_ThirdFailure_RetryIn1Day() {
        // Given: Domain with 2 failures
        UUID domainId = UUID.randomUUID();
        Domain domain = createDomain("flaky.com", DomainStatus.PROCESSING_FAILED);
        domain.setDomainId(domainId);
        domain.setFailureCount(2);
        when(domainRepository.findById(domainId)).thenReturn(Optional.of(domain));

        // When: Record third failure
        domainRegistryService.recordProcessingFailure(domainId, "DNS resolution failed");

        // Then: Retry in 1 day
        verify(domainRepository).save(domainCaptor.capture());
        Domain saved = domainCaptor.getValue();
        assertThat(saved.getFailureCount()).isEqualTo(3);
        assertThat(saved.getRetryAfter()).isBetween(
            now.plusDays(1).minusSeconds(5),
            now.plusDays(1).plusSeconds(5)
        );
    }

    @Test
    void testRecordProcessingFailure_FourthFailure_RetryIn1Week() {
        // Given: Domain with 3 failures
        UUID domainId = UUID.randomUUID();
        Domain domain = createDomain("flaky.com", DomainStatus.PROCESSING_FAILED);
        domain.setDomainId(domainId);
        domain.setFailureCount(3);
        when(domainRepository.findById(domainId)).thenReturn(Optional.of(domain));

        // When: Record fourth failure
        domainRegistryService.recordProcessingFailure(domainId, "Site permanently offline");

        // Then: Retry in 1 week
        verify(domainRepository).save(domainCaptor.capture());
        Domain saved = domainCaptor.getValue();
        assertThat(saved.getFailureCount()).isEqualTo(4);
        assertThat(saved.getRetryAfter()).isBetween(
            now.plusWeeks(1).minusSeconds(5),
            now.plusWeeks(1).plusSeconds(5)
        );
    }

    @Test
    void testRecordProcessingFailure_FifthFailure_StillRetryIn1Week() {
        // Given: Domain with 4+ failures
        UUID domainId = UUID.randomUUID();
        Domain domain = createDomain("flaky.com", DomainStatus.PROCESSING_FAILED);
        domain.setDomainId(domainId);
        domain.setFailureCount(4);
        when(domainRepository.findById(domainId)).thenReturn(Optional.of(domain));

        // When: Record fifth failure
        domainRegistryService.recordProcessingFailure(domainId, "Persistent failure");

        // Then: Still retry in 1 week (max backoff)
        verify(domainRepository).save(domainCaptor.capture());
        Domain saved = domainCaptor.getValue();
        assertThat(saved.getFailureCount()).isEqualTo(5);
        assertThat(saved.getRetryAfter()).isBetween(
            now.plusWeeks(1).minusSeconds(5),
            now.plusWeeks(1).plusSeconds(5)
        );
    }

    @Test
    void testRecordProcessingFailure_NonExistentDomain_DoesNothing() {
        // Given: Domain does not exist
        UUID domainId = UUID.randomUUID();
        when(domainRepository.findById(domainId)).thenReturn(Optional.empty());

        // When: Record failure
        domainRegistryService.recordProcessingFailure(domainId, "Failure");

        // Then: No save operation
        verify(domainRepository, never()).save(any());
    }

    // ===== Test getDomainByName and getDomainById =====

    @Test
    void testGetDomainByName_ExistingDomain_ReturnsDomain() {
        // Given: Domain exists
        Domain domain = createDomain("example.org", DomainStatus.DISCOVERED);
        when(domainRepository.findByDomainName("example.org")).thenReturn(Optional.of(domain));

        // When: Get domain by name
        Optional<Domain> result = domainRegistryService.getDomainByName("example.org");

        // Then: Domain returned
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(domain);
    }

    @Test
    void testGetDomainByName_NonExistentDomain_ReturnsEmpty() {
        // Given: Domain does not exist
        when(domainRepository.findByDomainName("nonexistent.org")).thenReturn(Optional.empty());

        // When: Get domain by name
        Optional<Domain> result = domainRegistryService.getDomainByName("nonexistent.org");

        // Then: Empty result
        assertThat(result).isEmpty();
    }

    @Test
    void testGetDomainById_ExistingDomain_ReturnsDomain() {
        // Given: Domain exists
        UUID domainId = UUID.randomUUID();
        Domain domain = createDomain("example.org", DomainStatus.DISCOVERED);
        domain.setDomainId(domainId);
        when(domainRepository.findById(domainId)).thenReturn(Optional.of(domain));

        // When: Get domain by ID
        Optional<Domain> result = domainRegistryService.getDomainById(domainId);

        // Then: Domain returned
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(domain);
    }

    @Test
    void testGetDomainById_NonExistentDomain_ReturnsEmpty() {
        // Given: Domain does not exist
        UUID domainId = UUID.randomUUID();
        when(domainRepository.findById(domainId)).thenReturn(Optional.empty());

        // When: Get domain by ID
        Optional<Domain> result = domainRegistryService.getDomainById(domainId);

        // Then: Empty result
        assertThat(result).isEmpty();
    }

    // ===== Helper Methods =====

    private Domain createDomain(String domainName, DomainStatus status) {
        return Domain.builder()
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
