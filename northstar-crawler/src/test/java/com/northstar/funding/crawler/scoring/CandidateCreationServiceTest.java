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
}
