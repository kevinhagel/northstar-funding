package com.northstar.funding.persistence.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.northstar.funding.domain.Domain;
import com.northstar.funding.domain.DomainStatus;
import com.northstar.funding.persistence.repository.DomainRepository;

/**
 * Unit tests for DomainService using Mockito.
 */
@ExtendWith(MockitoExtension.class)
class DomainServiceTest {

    @Mock
    private DomainRepository domainRepository;

    @InjectMocks
    private DomainService domainService;

    private Domain testDomain;
    private UUID testSessionId;

    @BeforeEach
    void setUp() {
        testSessionId = UUID.randomUUID();
        testDomain = Domain.builder()
            .domainId(UUID.randomUUID())
            .domainName("test.org")
            .status(DomainStatus.DISCOVERED)
            .discoverySessionId(testSessionId)
            .discoveredAt(LocalDateTime.now())
            .highQualityCandidateCount(0)
            .lowQualityCandidateCount(0)
            .build();
    }

    // ============================================================================
    // CREATE & UPDATE Operations Tests
    // ============================================================================

    @Test
    void registerDomain_WhenNew_ShouldCreateDomain() {
        // Given
        when(domainRepository.findByDomainName("test.org"))
            .thenReturn(Optional.empty());
        when(domainRepository.save(any(Domain.class)))
            .thenReturn(testDomain);

        // When
        Domain result = domainService.registerDomain("test.org", testSessionId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getDomainName()).isEqualTo("test.org");
        verify(domainRepository).findByDomainName("test.org");
        verify(domainRepository).save(any(Domain.class));
    }

    @Test
    void registerDomain_WhenExists_ShouldReturnExisting() {
        // Given
        when(domainRepository.findByDomainName("test.org"))
            .thenReturn(Optional.of(testDomain));

        // When
        Domain result = domainService.registerDomain("test.org", testSessionId);

        // Then
        assertThat(result).isEqualTo(testDomain);
        verify(domainRepository).findByDomainName("test.org");
        verify(domainRepository, never()).save(any(Domain.class));
    }

    @Test
    void updateStatus_WhenDomainExists_ShouldUpdateStatus() {
        // Given
        UUID domainId = testDomain.getDomainId();
        when(domainRepository.findById(domainId))
            .thenReturn(Optional.of(testDomain));
        when(domainRepository.save(any(Domain.class)))
            .thenReturn(testDomain);

        // When
        Domain result = domainService.updateStatus(domainId, DomainStatus.PROCESSED_HIGH_QUALITY);

        // Then
        assertThat(result.getStatus()).isEqualTo(DomainStatus.PROCESSED_HIGH_QUALITY);
        assertThat(result.getLastProcessedAt()).isNotNull();
        verify(domainRepository).findById(domainId);
        verify(domainRepository).save(testDomain);
    }

    @Test
    void updateStatus_WhenDomainNotFound_ShouldThrowException() {
        // Given
        UUID domainId = UUID.randomUUID();
        when(domainRepository.findById(domainId))
            .thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> domainService.updateStatus(domainId, DomainStatus.BLACKLISTED))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Domain not found");
        verify(domainRepository, never()).save(any(Domain.class));
    }

    @Test
    void blacklistDomain_WhenDomainExists_ShouldBlacklist() {
        // Given
        UUID adminId = UUID.randomUUID();
        String reason = "Spam domain";
        when(domainRepository.findByDomainName("test.org"))
            .thenReturn(Optional.of(testDomain));
        when(domainRepository.save(any(Domain.class)))
            .thenReturn(testDomain);

        // When
        Domain result = domainService.blacklistDomain("test.org", adminId, reason);

        // Then
        assertThat(result.getStatus()).isEqualTo(DomainStatus.BLACKLISTED);
        assertThat(result.getBlacklistedAt()).isNotNull();
        assertThat(result.getBlacklistedBy()).isEqualTo(adminId);
        assertThat(result.getBlacklistReason()).isEqualTo(reason);
        verify(domainRepository).save(testDomain);
    }

    @Test
    void updateCandidateCounts_ShouldUpdateCountsAndStatus() {
        // Given
        when(domainRepository.findByDomainName("test.org"))
            .thenReturn(Optional.of(testDomain));
        when(domainRepository.save(any(Domain.class)))
            .thenReturn(testDomain);

        BigDecimal confidence = new BigDecimal("0.85");

        // When
        domainService.updateCandidateCounts("test.org", 5, 2, confidence);

        // Then
        assertThat(testDomain.getHighQualityCandidateCount()).isEqualTo(5);
        assertThat(testDomain.getLowQualityCandidateCount()).isEqualTo(2);
        assertThat(testDomain.getBestConfidenceScore()).isEqualTo(confidence);
        assertThat(testDomain.getStatus()).isEqualTo(DomainStatus.PROCESSED_HIGH_QUALITY);
        verify(domainRepository).save(testDomain);
    }

    @Test
    void updateCandidateCounts_WithOnlyLowQuality_ShouldSetLowQualityStatus() {
        // Given
        when(domainRepository.findByDomainName("test.org"))
            .thenReturn(Optional.of(testDomain));
        when(domainRepository.save(any(Domain.class)))
            .thenReturn(testDomain);

        // When
        domainService.updateCandidateCounts("test.org", 0, 3, null);

        // Then
        assertThat(testDomain.getStatus()).isEqualTo(DomainStatus.PROCESSED_LOW_QUALITY);
        verify(domainRepository).save(testDomain);
    }

    // ============================================================================
    // READ Operations Tests
    // ============================================================================

    @Test
    void domainExists_WhenExists_ShouldReturnTrue() {
        // Given
        when(domainRepository.existsByDomainName("test.org"))
            .thenReturn(true);

        // When
        boolean result = domainService.domainExists("test.org");

        // Then
        assertThat(result).isTrue();
        verify(domainRepository).existsByDomainName("test.org");
    }

    @Test
    void domainExists_WhenNotExists_ShouldReturnFalse() {
        // Given
        when(domainRepository.existsByDomainName("test.org"))
            .thenReturn(false);

        // When
        boolean result = domainService.domainExists("test.org");

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void findByDomainName_WhenExists_ShouldReturnDomain() {
        // Given
        when(domainRepository.findByDomainName("test.org"))
            .thenReturn(Optional.of(testDomain));

        // When
        Optional<Domain> result = domainService.findByDomainName("test.org");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(testDomain);
    }

    @Test
    void findById_WhenExists_ShouldReturnDomain() {
        // Given
        UUID domainId = testDomain.getDomainId();
        when(domainRepository.findById(domainId))
            .thenReturn(Optional.of(testDomain));

        // When
        Optional<Domain> result = domainService.findById(domainId);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(testDomain);
    }

    @Test
    void getBlacklistedDomains_ShouldReturnBlacklistedDomains() {
        // Given
        List<Domain> blacklisted = List.of(testDomain);
        when(domainRepository.findByStatus(DomainStatus.BLACKLISTED))
            .thenReturn(blacklisted);

        // When
        List<Domain> result = domainService.getBlacklistedDomains();

        // Then
        assertThat(result).hasSize(1);
        assertThat(result).containsExactly(testDomain);
    }

    @Test
    void getHighQualityDomains_ShouldReturnHighQualityDomains() {
        // Given
        int minCandidates = 5;
        List<Domain> highQuality = List.of(testDomain);
        when(domainRepository.findHighQualityDomains(minCandidates))
            .thenReturn(highQuality);

        // When
        List<Domain> result = domainService.getHighQualityDomains(minCandidates);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result).containsExactly(testDomain);
    }

    @Test
    void getDomainsReadyForRetry_ShouldReturnEligibleDomains() {
        // Given
        List<Domain> retry = List.of(testDomain);
        when(domainRepository.findDomainsReadyForRetry(any(LocalDateTime.class)))
            .thenReturn(retry);

        // When
        List<Domain> result = domainService.getDomainsReadyForRetry();

        // Then
        assertThat(result).hasSize(1);
        assertThat(result).containsExactly(testDomain);
    }

    @Test
    void getDomainsBySession_ShouldReturnSessionDomains() {
        // Given
        List<Domain> sessionDomains = List.of(testDomain);
        when(domainRepository.findByDiscoverySessionId(testSessionId))
            .thenReturn(sessionDomains);

        // When
        List<Domain> result = domainService.getDomainsBySession(testSessionId);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result).containsExactly(testDomain);
    }

    @Test
    void countByStatus_ShouldReturnCount() {
        // Given
        when(domainRepository.countByStatus(DomainStatus.DISCOVERED))
            .thenReturn(42L);

        // When
        long result = domainService.countByStatus(DomainStatus.DISCOVERED);

        // Then
        assertThat(result).isEqualTo(42L);
    }

    @Test
    void getAverageConfidenceScore_ShouldReturnAverage() {
        // Given
        when(domainRepository.getAverageConfidenceScore())
            .thenReturn(0.75);

        // When
        Double result = domainService.getAverageConfidenceScore();

        // Then
        assertThat(result).isEqualTo(0.75);
    }

    @Test
    void searchDomains_ShouldReturnMatchingDomains() {
        // Given
        String pattern = "%test%";
        List<Domain> matches = List.of(testDomain);
        when(domainRepository.searchByDomainNamePattern(pattern))
            .thenReturn(matches);

        // When
        List<Domain> result = domainService.searchDomains(pattern);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result).containsExactly(testDomain);
    }
}
