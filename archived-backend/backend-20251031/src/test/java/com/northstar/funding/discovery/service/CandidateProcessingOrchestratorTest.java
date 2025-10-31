package com.northstar.funding.discovery.service;

import com.northstar.funding.discovery.domain.CandidateStatus;
import com.northstar.funding.discovery.domain.Domain;
import com.northstar.funding.discovery.domain.DomainStatus;
import com.northstar.funding.discovery.domain.FundingSourceCandidate;
import com.northstar.funding.discovery.infrastructure.FundingSourceCandidateRepository;
import com.northstar.funding.discovery.service.dto.MetadataJudgment;
import com.northstar.funding.discovery.service.dto.ProcessingStats;
import com.northstar.funding.discovery.service.dto.SearchResult;
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
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CandidateProcessingOrchestrator
 *
 * Tests Phase 1 orchestration with domain registration, metadata judging, and candidate creation.
 * Uses Mockito to mock dependencies.
 *
 * Critical TDD Flow:
 * 1. Write this test FIRST (T005)
 * 2. Tests should PASS (service already implemented)
 * 3. Verify all orchestration logic and Virtual Thread processing
 */
@ExtendWith(MockitoExtension.class)
class CandidateProcessingOrchestratorTest {

    @Mock
    private DomainRegistryService domainRegistryService;

    @Mock
    private MetadataJudgingService metadataJudgingService;

    @Mock
    private FundingSourceCandidateRepository candidateRepository;

    @InjectMocks
    private CandidateProcessingOrchestrator orchestrator;

    @Captor
    private ArgumentCaptor<FundingSourceCandidate> candidateCaptor;

    private UUID sessionId;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        sessionId = UUID.randomUUID();
        now = LocalDateTime.now();
    }

    // ===== Test processSearchResult - High Confidence Scenario =====

    @Test
    void testProcessSearchResult_HighConfidence_CreatesCandidateWithPendingCrawlStatus() {
        // Given: Search result with high confidence
        SearchResult searchResult = createSearchResult(
            "https://us-bulgaria.org/grants",
            "Bulgaria Education Grant Program",
            "Support for education initiatives in Bulgaria."
        );

        Domain domain = createDomain("us-bulgaria.org", DomainStatus.DISCOVERED);
        domain.setDomainId(UUID.randomUUID());

        MetadataJudgment judgment = createHighConfidenceJudgment(new BigDecimal("0.85"));

        when(domainRegistryService.shouldProcessDomain(searchResult.getUrl())).thenReturn(true);
        when(domainRegistryService.registerDomainFromUrl(searchResult.getUrl(), sessionId))
            .thenReturn(Try.success(domain));
        when(metadataJudgingService.judgeSearchResult(searchResult)).thenReturn(judgment);
        when(candidateRepository.save(any(FundingSourceCandidate.class)))
            .thenAnswer(invocation -> {
                FundingSourceCandidate candidate = invocation.getArgument(0);
                candidate.setCandidateId(UUID.randomUUID());
                return candidate;
            });

        // When: Process search result
        orchestrator.processSearchResult(searchResult, sessionId);

        // Then: Candidate created with PENDING_CRAWL status
        verify(candidateRepository).save(candidateCaptor.capture());
        FundingSourceCandidate saved = candidateCaptor.getValue();

        assertThat(saved.getStatus()).isEqualTo(CandidateStatus.PENDING_CRAWL);
        assertThat(saved.getConfidenceScore()).isEqualByComparingTo(new BigDecimal("0.85"));
        assertThat(saved.getDomainId()).isEqualTo(domain.getDomainId());
        assertThat(saved.getDiscoverySessionId()).isEqualTo(sessionId);
        assertThat(saved.getSourceUrl()).isEqualTo(searchResult.getUrl());
        assertThat(saved.getOrganizationName()).isEqualTo(judgment.getExtractedOrganizationName());
        assertThat(saved.getProgramName()).isEqualTo(judgment.getExtractedProgramName());
        assertThat(saved.getDescription()).isEqualTo(searchResult.getSnippet());
        assertThat(saved.getDiscoveryMethod()).isEqualTo("METADATA_JUDGING");
        assertThat(saved.getSearchQuery()).isEqualTo(searchResult.getSearchQuery());
        assertThat(saved.getValidationNotes()).isEqualTo(judgment.getReasoning());
        assertThat(saved.getDiscoveredAt()).isNotNull();
        assertThat(saved.getLastUpdatedAt()).isNotNull();

        // Verify domain quality updated (high quality)
        verify(domainRegistryService).updateDomainQuality(
            domain.getDomainId(),
            new BigDecimal("0.85"),
            true
        );
    }

    @Test
    void testProcessSearchResult_HighConfidence_UpdatesDomainQualityAsHighQuality() {
        // Given: High confidence result
        SearchResult searchResult = createSearchResult(
            "https://example.org/funding",
            "Grant Program",
            "Scholarships available."
        );

        Domain domain = createDomain("example.org", DomainStatus.DISCOVERED);
        domain.setDomainId(UUID.randomUUID());

        MetadataJudgment judgment = createHighConfidenceJudgment(new BigDecimal("0.75"));

        when(domainRegistryService.shouldProcessDomain(anyString())).thenReturn(true);
        when(domainRegistryService.registerDomainFromUrl(anyString(), any(UUID.class)))
            .thenReturn(Try.success(domain));
        when(metadataJudgingService.judgeSearchResult(any())).thenReturn(judgment);
        when(candidateRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // When: Process search result
        orchestrator.processSearchResult(searchResult, sessionId);

        // Then: Domain quality updated with isHighQuality=true
        verify(domainRegistryService).updateDomainQuality(
            domain.getDomainId(),
            new BigDecimal("0.75"),
            true // high quality
        );
    }

    // ===== Test processSearchResult - Low Confidence Scenario =====

    @Test
    void testProcessSearchResult_LowConfidence_SkipsAndUpdatesDomainQuality() {
        // Given: Search result with low confidence
        SearchResult searchResult = createSearchResult(
            "https://example.com/about",
            "About Our Company",
            "We are a technology company."
        );

        Domain domain = createDomain("example.com", DomainStatus.DISCOVERED);
        domain.setDomainId(UUID.randomUUID());

        MetadataJudgment judgment = createLowConfidenceJudgment(new BigDecimal("0.35"));

        when(domainRegistryService.shouldProcessDomain(searchResult.getUrl())).thenReturn(true);
        when(domainRegistryService.registerDomainFromUrl(searchResult.getUrl(), sessionId))
            .thenReturn(Try.success(domain));
        when(metadataJudgingService.judgeSearchResult(searchResult)).thenReturn(judgment);

        // When: Process search result
        orchestrator.processSearchResult(searchResult, sessionId);

        // Then: No candidate created
        verify(candidateRepository, never()).save(any());

        // Domain quality updated with isHighQuality=false
        verify(domainRegistryService).updateDomainQuality(
            domain.getDomainId(),
            new BigDecimal("0.35"),
            false // low quality
        );
    }

    // ===== Test processSearchResult - Domain Skipping =====

    @Test
    void testProcessSearchResult_BlacklistedDomain_SkipsEarly() {
        // Given: Blacklisted domain
        SearchResult searchResult = createSearchResult(
            "https://spam.com/grants",
            "Free Grants!",
            "Click here for free money."
        );

        when(domainRegistryService.shouldProcessDomain(searchResult.getUrl())).thenReturn(false);
        when(domainRegistryService.extractDomainName(searchResult.getUrl()))
            .thenReturn(Try.success("spam.com"));

        // When: Process search result
        orchestrator.processSearchResult(searchResult, sessionId);

        // Then: Skipped early - no registration, no judging, no candidate creation
        verify(domainRegistryService, never()).registerDomainFromUrl(anyString(), any());
        verify(metadataJudgingService, never()).judgeSearchResult(any());
        verify(candidateRepository, never()).save(any());
        verify(domainRegistryService, never()).updateDomainQuality(any(), any(BigDecimal.class), anyBoolean());
    }

    @Test
    void testProcessSearchResult_DomainAlreadyProcessed_SkipsEarly() {
        // Given: Domain already processed
        SearchResult searchResult = createSearchResult(
            "https://example.org/another-page",
            "Another Page",
            "More information."
        );

        when(domainRegistryService.shouldProcessDomain(searchResult.getUrl())).thenReturn(false);
        when(domainRegistryService.extractDomainName(searchResult.getUrl()))
            .thenReturn(Try.success("example.org"));

        // When: Process search result
        orchestrator.processSearchResult(searchResult, sessionId);

        // Then: Skipped early
        verify(domainRegistryService, never()).registerDomainFromUrl(anyString(), any());
        verify(metadataJudgingService, never()).judgeSearchResult(any());
        verify(candidateRepository, never()).save(any());
    }

    // ===== Test processSearchResult - Domain Registration Failure =====

    @Test
    void testProcessSearchResult_DomainRegistrationFails_ReturnsFailure() {
        // Given: Domain registration fails
        SearchResult searchResult = createSearchResult(
            "https://invalid-url",
            "Invalid URL",
            "Test snippet."
        );

        when(domainRegistryService.shouldProcessDomain(searchResult.getUrl())).thenReturn(true);
        when(domainRegistryService.registerDomainFromUrl(searchResult.getUrl(), sessionId))
            .thenReturn(Try.failure(new IllegalArgumentException("Invalid URL")));

        // When: Process search result
        orchestrator.processSearchResult(searchResult, sessionId);

        // Then: Returns failure, no judging, no candidate creation
        verify(metadataJudgingService, never()).judgeSearchResult(any());
        verify(candidateRepository, never()).save(any());
        verify(domainRegistryService, never()).updateDomainQuality(any(), any(BigDecimal.class), anyBoolean());
    }

    // ===== Test processSearchResult - Exception Handling =====

    @Test
    void testProcessSearchResult_JudgingThrowsException_ReturnsFailure() {
        // Given: Metadata judging throws exception
        SearchResult searchResult = createSearchResult(
            "https://example.org/test",
            "Test Title",
            "Test snippet."
        );

        Domain domain = createDomain("example.org", DomainStatus.DISCOVERED);
        domain.setDomainId(UUID.randomUUID());

        when(domainRegistryService.shouldProcessDomain(anyString())).thenReturn(true);
        when(domainRegistryService.registerDomainFromUrl(anyString(), any()))
            .thenReturn(Try.success(domain));
        when(metadataJudgingService.judgeSearchResult(any()))
            .thenThrow(new RuntimeException("Judging error"));

        // When: Process search result
        orchestrator.processSearchResult(searchResult, sessionId);

        // Then: Returns failure, no candidate created
        verify(candidateRepository, never()).save(any());
    }

    // ===== Test processSearchResults - Multiple Results =====

    @Test
    void testProcessSearchResults_MultipleResults_ProcessesAllInParallel() {
        // Given: Multiple search results with varying confidence
        SearchResult result1 = createSearchResult(
            "https://high-confidence.org/grant",
            "Grant Program - Foundation",
            "Scholarships and grants for Bulgaria students."
        );

        SearchResult result2 = createSearchResult(
            "https://low-confidence.com/page",
            "About Us",
            "We are a company."
        );

        SearchResult result3 = createSearchResult(
            "https://another-high.org/funding",
            "Funding Opportunities - NGO",
            "Fellowships available in Eastern Europe."
        );

        List<SearchResult> searchResults = List.of(result1, result2, result3);

        // Mock high confidence for result1 and result3
        Domain domain1 = createDomain("high-confidence.org", DomainStatus.DISCOVERED);
        domain1.setDomainId(UUID.randomUUID());
        Domain domain2 = createDomain("low-confidence.com", DomainStatus.DISCOVERED);
        domain2.setDomainId(UUID.randomUUID());
        Domain domain3 = createDomain("another-high.org", DomainStatus.DISCOVERED);
        domain3.setDomainId(UUID.randomUUID());

        when(domainRegistryService.shouldProcessDomain(anyString())).thenReturn(true);
        when(domainRegistryService.registerDomainFromUrl(contains("high-confidence.org"), any()))
            .thenReturn(Try.success(domain1));
        when(domainRegistryService.registerDomainFromUrl(contains("low-confidence.com"), any()))
            .thenReturn(Try.success(domain2));
        when(domainRegistryService.registerDomainFromUrl(contains("another-high.org"), any()))
            .thenReturn(Try.success(domain3));

        when(metadataJudgingService.judgeSearchResult(argThat(sr ->
            sr != null && sr.getUrl().contains("high-confidence.org"))))
            .thenReturn(createHighConfidenceJudgment(new BigDecimal("0.85")));
        when(metadataJudgingService.judgeSearchResult(argThat(sr ->
            sr != null && sr.getUrl().contains("low-confidence.com"))))
            .thenReturn(createLowConfidenceJudgment(new BigDecimal("0.35")));
        when(metadataJudgingService.judgeSearchResult(argThat(sr ->
            sr != null && sr.getUrl().contains("another-high.org"))))
            .thenReturn(createHighConfidenceJudgment(new BigDecimal("0.75")));

        when(candidateRepository.save(any())).thenAnswer(inv -> {
            FundingSourceCandidate c = inv.getArgument(0);
            c.setCandidateId(UUID.randomUUID());
            return c;
        });

        // When: Process all results
        ProcessingStats stats = orchestrator.processSearchResults(searchResults, sessionId);

        // Then: Statistics aggregated correctly
        assertThat(stats.getTotalProcessed()).isEqualTo(3);
        assertThat(stats.getCandidatesCreated()).isEqualTo(2); // result1 and result3
        assertThat(stats.getSkippedLowConfidence()).isEqualTo(1); // result2
        assertThat(stats.getFailures()).isEqualTo(0);

        // Verify confidence statistics using BigDecimal
        BigDecimal expectedAverage = new BigDecimal("0.85").add(new BigDecimal("0.75"))
            .divide(new BigDecimal("2"), 2, java.math.RoundingMode.HALF_UP);
        assertThat(stats.getAverageConfidence()).isEqualByComparingTo(expectedAverage);
        assertThat(stats.getMaxConfidence()).isEqualByComparingTo(new BigDecimal("0.85"));
        assertThat(stats.getMinConfidence()).isEqualByComparingTo(new BigDecimal("0.75"));
        assertThat(stats.getProcessingTimeMs()).isGreaterThanOrEqualTo(0L); // Can be 0 for very fast processing

        // Verify 2 candidates saved
        verify(candidateRepository, times(2)).save(any());
    }

    @Test
    void testProcessSearchResults_AllHighConfidence_CreatesAllCandidates() {
        // Given: All high confidence results
        List<SearchResult> searchResults = List.of(
            createSearchResult("https://example1.org/grant", "Grant 1", "Scholarship program."),
            createSearchResult("https://example2.org/funding", "Grant 2", "Fellowship opportunities."),
            createSearchResult("https://example3.org/awards", "Grant 3", "Awards for students.")
        );

        setupMocksForHighConfidence();

        // When: Process all results
        ProcessingStats stats = orchestrator.processSearchResults(searchResults, sessionId);

        // Then: All candidates created
        assertThat(stats.getTotalProcessed()).isEqualTo(3);
        assertThat(stats.getCandidatesCreated()).isEqualTo(3);
        assertThat(stats.getSkippedLowConfidence()).isEqualTo(0);
        assertThat(stats.getFailures()).isEqualTo(0);
        verify(candidateRepository, times(3)).save(any());
    }

    @Test
    void testProcessSearchResults_AllLowConfidence_SkipsAll() {
        // Given: All low confidence results
        List<SearchResult> searchResults = List.of(
            createSearchResult("https://example1.com/page", "Page 1", "Company info."),
            createSearchResult("https://example2.com/about", "Page 2", "About us."),
            createSearchResult("https://example3.com/contact", "Page 3", "Contact us.")
        );

        setupMocksForLowConfidence();

        // When: Process all results
        ProcessingStats stats = orchestrator.processSearchResults(searchResults, sessionId);

        // Then: All skipped
        assertThat(stats.getTotalProcessed()).isEqualTo(3);
        assertThat(stats.getCandidatesCreated()).isEqualTo(0);
        assertThat(stats.getSkippedLowConfidence()).isEqualTo(3);
        assertThat(stats.getFailures()).isEqualTo(0);
        verify(candidateRepository, never()).save(any());
    }

    @Test
    void testProcessSearchResults_MixedWithBlacklisted_AggregatesCorrectly() {
        // Given: Mix of high confidence, low confidence, and blacklisted
        List<SearchResult> searchResults = List.of(
            createSearchResult("https://good.org/grant", "Grant", "Scholarship."), // High
            createSearchResult("https://spam.com/offer", "Offer", "Click here."), // Blacklisted
            createSearchResult("https://bad.com/page", "Page", "Company.")        // Low
        );

        Domain domain1 = createDomain("good.org", DomainStatus.DISCOVERED);
        domain1.setDomainId(UUID.randomUUID());
        Domain domain3 = createDomain("bad.com", DomainStatus.DISCOVERED);
        domain3.setDomainId(UUID.randomUUID());

        when(domainRegistryService.shouldProcessDomain(contains("good.org"))).thenReturn(true);
        when(domainRegistryService.shouldProcessDomain(contains("spam.com"))).thenReturn(false);
        when(domainRegistryService.shouldProcessDomain(contains("bad.com"))).thenReturn(true);

        when(domainRegistryService.extractDomainName(contains("spam.com")))
            .thenReturn(Try.success("spam.com"));

        when(domainRegistryService.registerDomainFromUrl(contains("good.org"), any()))
            .thenReturn(Try.success(domain1));
        when(domainRegistryService.registerDomainFromUrl(contains("bad.com"), any()))
            .thenReturn(Try.success(domain3));

        when(metadataJudgingService.judgeSearchResult(argThat(sr -> sr != null && sr.getUrl().contains("good.org"))))
            .thenReturn(createHighConfidenceJudgment(new BigDecimal("0.80")));
        when(metadataJudgingService.judgeSearchResult(argThat(sr -> sr != null && sr.getUrl().contains("bad.com"))))
            .thenReturn(createLowConfidenceJudgment(new BigDecimal("0.40")));

        when(candidateRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // When: Process all results
        ProcessingStats stats = orchestrator.processSearchResults(searchResults, sessionId);

        // Then: Aggregated correctly
        assertThat(stats.getTotalProcessed()).isEqualTo(3);
        assertThat(stats.getCandidatesCreated()).isEqualTo(1); // good.org
        assertThat(stats.getSkippedLowConfidence()).isEqualTo(1); // bad.com
        assertThat(stats.getSkippedDomainAlreadyProcessed()).isEqualTo(1); // spam.com
        assertThat(stats.getFailures()).isEqualTo(0);
    }

    @Test
    void testProcessSearchResults_WithFailures_AggregatesCorrectly() {
        // Given: Some results fail during processing
        List<SearchResult> searchResults = List.of(
            createSearchResult("https://good.org/grant", "Grant", "Scholarship."), // Success
            createSearchResult("https://fail.org/page", "Page", "Test.")          // Failure
        );

        Domain domain1 = createDomain("good.org", DomainStatus.DISCOVERED);
        domain1.setDomainId(UUID.randomUUID());

        when(domainRegistryService.shouldProcessDomain(anyString())).thenReturn(true);
        when(domainRegistryService.registerDomainFromUrl(contains("good.org"), any()))
            .thenReturn(Try.success(domain1));
        when(domainRegistryService.registerDomainFromUrl(contains("fail.org"), any()))
            .thenReturn(Try.failure(new RuntimeException("Registration failed")));

        when(metadataJudgingService.judgeSearchResult(argThat(sr -> sr != null && sr.getUrl().contains("good.org"))))
            .thenReturn(createHighConfidenceJudgment(new BigDecimal("0.80")));

        when(candidateRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // When: Process all results
        ProcessingStats stats = orchestrator.processSearchResults(searchResults, sessionId);

        // Then: Aggregated correctly
        assertThat(stats.getTotalProcessed()).isEqualTo(2);
        assertThat(stats.getCandidatesCreated()).isEqualTo(1);
        assertThat(stats.getFailures()).isEqualTo(1);
    }

    @Test
    void testProcessSearchResults_EmptyList_ReturnsZeroStats() {
        // Given: Empty list
        List<SearchResult> searchResults = List.of();

        // When: Process empty list
        ProcessingStats stats = orchestrator.processSearchResults(searchResults, sessionId);

        // Then: Zero statistics
        assertThat(stats.getTotalProcessed()).isEqualTo(0);
        assertThat(stats.getCandidatesCreated()).isEqualTo(0);
        assertThat(stats.getSkippedLowConfidence()).isEqualTo(0);
        assertThat(stats.getFailures()).isEqualTo(0);
        assertThat(stats.getAverageConfidence()).isNull();
        assertThat(stats.getMaxConfidence()).isNull();
        assertThat(stats.getMinConfidence()).isNull();
    }

    @Test
    void testProcessSearchResults_GetSummary_ReturnsFormattedString() {
        // Given: Some processing results
        List<SearchResult> searchResults = List.of(
            createSearchResult("https://good.org/grant", "Grant", "Scholarship.")
        );

        setupMocksForHighConfidence();

        // When: Process results
        ProcessingStats stats = orchestrator.processSearchResults(searchResults, sessionId);

        // Then: Summary string is formatted correctly
        String summary = stats.getSummary();
        assertThat(summary).contains("Processed 1 results");
        assertThat(summary).contains("1 candidates created");
        assertThat(summary).isNotBlank();
    }

    // ===== Helper Methods =====

    private SearchResult createSearchResult(String url, String title, String snippet) {
        return SearchResult.builder()
            .url(url)
            .title(title)
            .snippet(snippet)
            .searchEngine("searxng")
            .searchQuery("Bulgaria education grants")
            .position(1)
            .build();
    }

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

    private MetadataJudgment createHighConfidenceJudgment(BigDecimal confidence) {
        return MetadataJudgment.builder()
            .confidenceScore(confidence)
            .shouldCrawl(true)
            .domainName("example.org")
            .extractedOrganizationName("Example Foundation")
            .extractedProgramName("Education Grant Program")
            .reasoning("High confidence based on funding keywords and credible domain.")
            .build();
    }

    private MetadataJudgment createLowConfidenceJudgment(BigDecimal confidence) {
        return MetadataJudgment.builder()
            .confidenceScore(confidence)
            .shouldCrawl(false)
            .domainName("example.com")
            .extractedOrganizationName("Unknown Organization")
            .extractedProgramName("Unknown Program")
            .reasoning("Low confidence - no funding keywords detected.")
            .build();
    }

    private void setupMocksForHighConfidence() {
        when(domainRegistryService.shouldProcessDomain(anyString())).thenReturn(true);
        when(domainRegistryService.registerDomainFromUrl(anyString(), any())).thenAnswer(invocation -> {
            String url = invocation.getArgument(0);
            Domain domain = createDomain(url.replaceAll("https?://", "").split("/")[0], DomainStatus.DISCOVERED);
            domain.setDomainId(UUID.randomUUID());
            return Try.success(domain);
        });
        when(metadataJudgingService.judgeSearchResult(any()))
            .thenReturn(createHighConfidenceJudgment(new BigDecimal("0.80")));
        when(candidateRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private void setupMocksForLowConfidence() {
        when(domainRegistryService.shouldProcessDomain(anyString())).thenReturn(true);
        when(domainRegistryService.registerDomainFromUrl(anyString(), any())).thenAnswer(invocation -> {
            String url = invocation.getArgument(0);
            Domain domain = createDomain(url.replaceAll("https?://", "").split("/")[0], DomainStatus.DISCOVERED);
            domain.setDomainId(UUID.randomUUID());
            return Try.success(domain);
        });
        when(metadataJudgingService.judgeSearchResult(any()))
            .thenReturn(createLowConfidenceJudgment(new BigDecimal("0.40")));
    }
}
