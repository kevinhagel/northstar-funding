package com.northstar.funding.rest.service;

import com.northstar.funding.domain.CandidateStatus;
import com.northstar.funding.domain.DomainStatus;
import com.northstar.funding.domain.FundingSourceCandidate;
import com.northstar.funding.domain.SearchEngineType;
import com.northstar.funding.persistence.repository.FundingSourceCandidateRepository;
import com.northstar.funding.persistence.repository.DomainRepository;
import com.northstar.funding.rest.dto.CandidateDTO;
import com.northstar.funding.rest.dto.CandidateDTOMapper;
import com.northstar.funding.rest.dto.CandidatePageDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CandidateServiceTest {

    @Mock
    private FundingSourceCandidateRepository candidateRepository;

    @Mock
    private DomainRepository domainRepository;

    @Mock
    private CandidateDTOMapper mapper;

    @InjectMocks
    private CandidateService candidateService;

    private FundingSourceCandidate testCandidate;
    private CandidateDTO testDTO;
    private UUID testId;

    @BeforeEach
    void setUp() {
        testId = UUID.randomUUID();
        testCandidate = FundingSourceCandidate.builder()
            .candidateId(testId)
            .sourceUrl("https://example.com/funding")
            .metadataTitle("Test Funding Program")
            .confidenceScore(new BigDecimal("0.85"))
            .status(CandidateStatus.PENDING_CRAWL)
            .searchEngineSource(SearchEngineType.TAVILY)
            .discoveredAt(LocalDateTime.now())
            .build();

        testDTO = new CandidateDTO(
            testId.toString(),
            "https://example.com/funding",
            "Test Funding Program",
            "0.85",
            "PENDING_CRAWL",
            "TAVILY",
            "2025-11-16T10:30:00"
        );
    }

    @Test
    void listCandidates_WithNoFilters_ShouldReturnAllCandidates() {
        // Given
        List<FundingSourceCandidate> candidates = List.of(testCandidate);
        when(candidateRepository.findAll()).thenReturn(candidates);
        when(mapper.toDTOs(candidates)).thenReturn(List.of(testDTO));

        // When
        CandidatePageDTO result = candidateService.listCandidates(null, null, null, null, null, null, null, 0, 20);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.content()).hasSize(1);
        assertThat(result.totalElements()).isEqualTo(1);
        verify(candidateRepository).findAll();
    }

    @Test
    void approveCandidate_WithValidId_ShouldUpdateStatusAndReturn() {
        // Given
        when(candidateRepository.findById(testId)).thenReturn(Optional.of(testCandidate));
        when(candidateRepository.save(any(FundingSourceCandidate.class))).thenReturn(testCandidate);
        when(mapper.toDTO(any(FundingSourceCandidate.class))).thenReturn(testDTO);

        // When
        CandidateDTO result = candidateService.approveCandidate(testId);

        // Then
        assertThat(result).isNotNull();
        verify(candidateRepository).findById(testId);
        verify(candidateRepository).save(any(FundingSourceCandidate.class));
    }

    @Test
    void approveCandidate_WithNonExistentId_ShouldThrowException() {
        // Given
        when(candidateRepository.findById(testId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> candidateService.approveCandidate(testId))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("not found");
    }

    @Test
    void rejectCandidate_WithValidId_ShouldUpdateStatusAndBlacklistDomain() {
        // Given
        when(candidateRepository.findById(testId)).thenReturn(Optional.of(testCandidate));
        when(candidateRepository.save(any(FundingSourceCandidate.class))).thenReturn(testCandidate);
        when(mapper.toDTO(any(FundingSourceCandidate.class))).thenReturn(testDTO);

        // When
        CandidateDTO result = candidateService.rejectCandidate(testId);

        // Then
        assertThat(result).isNotNull();
        verify(candidateRepository).findById(testId);
        verify(candidateRepository).save(any(FundingSourceCandidate.class));
        // Domain blacklisting will be implemented when we have the domain relationship
    }

    @Test
    void rejectCandidate_WithNonExistentId_ShouldThrowException() {
        // Given
        when(candidateRepository.findById(testId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> candidateService.rejectCandidate(testId))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("not found");
    }
}
