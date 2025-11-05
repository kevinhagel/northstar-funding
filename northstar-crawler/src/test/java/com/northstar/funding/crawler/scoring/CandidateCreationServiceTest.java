package com.northstar.funding.crawler.scoring;

import com.northstar.funding.domain.CandidateStatus;
import com.northstar.funding.domain.FundingSourceCandidate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for CandidateCreationService
 * Tests candidate creation with confidence-based status assignment
 */
@ExtendWith(MockitoExtension.class)
class CandidateCreationServiceTest {

    private CandidateCreationService candidateCreationService;

    @BeforeEach
    void setUp() {
        candidateCreationService = new CandidateCreationService();
    }

    @Test
    @DisplayName("High confidence (>= 0.60) creates PENDING_CRAWL candidate")
    void testHighConfidenceCreatesPendingCrawl() {
        // Given: High confidence score >= 0.60
        String title = "EU Grants for Education";
        String description = "Apply for funding opportunities";
        String url = "https://example.org";
        UUID domainId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        BigDecimal confidence = new BigDecimal("0.75");

        // When
        FundingSourceCandidate candidate = candidateCreationService.createCandidate(
            title, description, url, domainId, sessionId, confidence
        );

        // Then
        assertThat(candidate).isNotNull();
        assertThat(candidate.getStatus()).isEqualTo(CandidateStatus.PENDING_CRAWL);
        assertThat(candidate.getOrganizationName()).isEqualTo(title);
        assertThat(candidate.getDescription()).isEqualTo(description);
        assertThat(candidate.getSourceUrl()).isEqualTo(url);
        assertThat(candidate.getDomainId()).isEqualTo(domainId);
        assertThat(candidate.getDiscoverySessionId()).isEqualTo(sessionId);
        assertThat(candidate.getConfidenceScore()).isEqualByComparingTo(confidence);
    }

    @Test
    @DisplayName("Low confidence (< 0.60) creates SKIPPED_LOW_CONFIDENCE candidate")
    void testLowConfidenceCreatesSkipped() {
        // Given: Low confidence score < 0.60
        String title = "Some website";
        String description = "Generic description";
        String url = "https://example.com";
        UUID domainId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        BigDecimal confidence = new BigDecimal("0.35");

        // When
        FundingSourceCandidate candidate = candidateCreationService.createCandidate(
            title, description, url, domainId, sessionId, confidence
        );

        // Then
        assertThat(candidate).isNotNull();
        assertThat(candidate.getStatus()).isEqualTo(CandidateStatus.SKIPPED_LOW_CONFIDENCE);
        assertThat(candidate.getOrganizationName()).isEqualTo(title);
        assertThat(candidate.getDescription()).isEqualTo(description);
        assertThat(candidate.getSourceUrl()).isEqualTo(url);
        assertThat(candidate.getDomainId()).isEqualTo(domainId);
        assertThat(candidate.getDiscoverySessionId()).isEqualTo(sessionId);
        assertThat(candidate.getConfidenceScore()).isEqualByComparingTo(confidence);
    }

    @Test
    @DisplayName("Exact threshold (0.60) creates PENDING_CRAWL candidate")
    void testExactThresholdCreatesPendingCrawl() {
        // Given: Exact confidence score = 0.60 (boundary case)
        String title = "Foundation Portal";
        String description = "Funding information";
        String url = "https://example.org";
        UUID domainId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        BigDecimal confidence = new BigDecimal("0.60");

        // When
        FundingSourceCandidate candidate = candidateCreationService.createCandidate(
            title, description, url, domainId, sessionId, confidence
        );

        // Then: Threshold is inclusive (>=), so 0.60 should create PENDING_CRAWL
        assertThat(candidate).isNotNull();
        assertThat(candidate.getStatus()).isEqualTo(CandidateStatus.PENDING_CRAWL);
        assertThat(candidate.getConfidenceScore()).isEqualByComparingTo(new BigDecimal("0.60"));
    }

    @Test
    @DisplayName("Null title and description handled gracefully")
    void testNullInputsHandled() {
        // Given: Null title and description
        UUID domainId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        BigDecimal confidence = new BigDecimal("0.75");

        // When
        FundingSourceCandidate candidate = candidateCreationService.createCandidate(
            null, null, "https://example.org", domainId, sessionId, confidence
        );

        // Then: Candidate created with null values
        assertThat(candidate).isNotNull();
        assertThat(candidate.getStatus()).isEqualTo(CandidateStatus.PENDING_CRAWL);
        assertThat(candidate.getOrganizationName()).isNull();
        assertThat(candidate.getDescription()).isNull();
        assertThat(candidate.getSourceUrl()).isEqualTo("https://example.org");
        assertThat(candidate.getConfidenceScore()).isEqualByComparingTo(confidence);
    }

    @Test
    @DisplayName("Empty strings handled like any other value")
    void testEmptyStringsHandled() {
        // Given: Empty strings for title and description
        UUID domainId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        BigDecimal confidence = new BigDecimal("0.65");

        // When
        FundingSourceCandidate candidate = candidateCreationService.createCandidate(
            "", "", "https://example.org", domainId, sessionId, confidence
        );

        // Then: Candidate created with empty strings preserved
        assertThat(candidate).isNotNull();
        assertThat(candidate.getStatus()).isEqualTo(CandidateStatus.PENDING_CRAWL);
        assertThat(candidate.getOrganizationName()).isEmpty();
        assertThat(candidate.getDescription()).isEmpty();
        assertThat(candidate.getSourceUrl()).isEqualTo("https://example.org");
        assertThat(candidate.getConfidenceScore()).isEqualByComparingTo(confidence);
    }
}
