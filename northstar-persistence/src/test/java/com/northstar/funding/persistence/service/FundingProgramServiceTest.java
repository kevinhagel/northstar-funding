package com.northstar.funding.persistence.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
import org.springframework.data.domain.PageRequest;

import com.northstar.funding.domain.FundingProgram;
import com.northstar.funding.domain.ProgramStatus;
import com.northstar.funding.persistence.repository.FundingProgramRepository;

/**
 * Unit tests for FundingProgramService using Mockito.
 */
@ExtendWith(MockitoExtension.class)
class FundingProgramServiceTest {

    @Mock
    private FundingProgramRepository fundingProgramRepository;

    @InjectMocks
    private FundingProgramService fundingProgramService;

    private FundingProgram testProgram;
    private UUID testOrganizationId;

    @BeforeEach
    void setUp() {
        testOrganizationId = UUID.randomUUID();
        testProgram = FundingProgram.builder()
            .programId(UUID.randomUUID())
            .organizationId(testOrganizationId)
            .domain("test.org")
            .programName("Test Program")
            .programUrl("https://test.org/program")
            .discoveredAt(LocalDateTime.now())
            .isActive(true)
            .build();
    }

    // ============================================================================
    // CREATE & UPDATE Operations Tests
    // ============================================================================

    @Test
    void registerProgram_WhenNew_ShouldCreateProgram() {
        // Given
        when(fundingProgramRepository.findByProgramUrl("https://test.org/program"))
            .thenReturn(Optional.empty());
        when(fundingProgramRepository.save(any(FundingProgram.class)))
            .thenReturn(testProgram);

        // When
        FundingProgram result = fundingProgramService.registerProgram(
            testOrganizationId, "test.org", "Test Program",
            "https://test.org/program", "Description");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getProgramName()).isEqualTo("Test Program");
        verify(fundingProgramRepository).findByProgramUrl("https://test.org/program");
        verify(fundingProgramRepository).save(any(FundingProgram.class));
    }

    @Test
    void registerProgram_WhenExists_ShouldReturnExisting() {
        // Given
        when(fundingProgramRepository.findByProgramUrl("https://test.org/program"))
            .thenReturn(Optional.of(testProgram));

        // When
        FundingProgram result = fundingProgramService.registerProgram(
            testOrganizationId, "test.org", "Test Program",
            "https://test.org/program", "Description");

        // Then
        assertThat(result).isEqualTo(testProgram);
        verify(fundingProgramRepository, never()).save(any(FundingProgram.class));
    }

    @Test
    void updateStatus_WhenProgramExists_ShouldUpdateStatus() {
        // Given
        UUID programId = testProgram.getProgramId();
        when(fundingProgramRepository.findById(programId))
            .thenReturn(Optional.of(testProgram));
        when(fundingProgramRepository.save(any(FundingProgram.class)))
            .thenReturn(testProgram);

        // When
        FundingProgram result = fundingProgramService.updateStatus(
            programId, ProgramStatus.ACTIVE);

        // Then
        assertThat(result.getStatus()).isEqualTo(ProgramStatus.ACTIVE);
        assertThat(result.getLastRefreshedAt()).isNotNull();
        verify(fundingProgramRepository).save(testProgram);
    }

    @Test
    void updateStatus_WhenProgramNotFound_ShouldThrowException() {
        // Given
        UUID programId = UUID.randomUUID();
        when(fundingProgramRepository.findById(programId))
            .thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> fundingProgramService.updateStatus(
            programId, ProgramStatus.ACTIVE))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Program not found");
        verify(fundingProgramRepository, never()).save(any(FundingProgram.class));
    }

    @Test
    void markAsValidFundingOpportunity_WhenProgramExists_ShouldMarkAsValid() {
        // Given
        UUID programId = testProgram.getProgramId();
        BigDecimal confidence = new BigDecimal("0.88");
        when(fundingProgramRepository.findById(programId))
            .thenReturn(Optional.of(testProgram));
        when(fundingProgramRepository.save(any(FundingProgram.class)))
            .thenReturn(testProgram);

        // When
        FundingProgram result = fundingProgramService.markAsValidFundingOpportunity(
            programId, confidence);

        // Then
        assertThat(result.getIsValidFundingOpportunity()).isTrue();
        assertThat(result.getProgramConfidence()).isEqualTo(confidence);
        assertThat(result.getLastRefreshedAt()).isNotNull();
        verify(fundingProgramRepository).save(testProgram);
    }

    @Test
    void updateDeadline_WhenProgramExists_ShouldUpdateDeadline() {
        // Given
        UUID programId = testProgram.getProgramId();
        LocalDateTime deadline = LocalDateTime.now().plusDays(30);
        when(fundingProgramRepository.findById(programId))
            .thenReturn(Optional.of(testProgram));
        when(fundingProgramRepository.save(any(FundingProgram.class)))
            .thenReturn(testProgram);

        // When
        FundingProgram result = fundingProgramService.updateDeadline(programId, deadline);

        // Then
        assertThat(result.getApplicationDeadline()).isEqualTo(deadline);
        assertThat(result.getLastRefreshedAt()).isNotNull();
        verify(fundingProgramRepository).save(testProgram);
    }

    @Test
    void markAsExpired_WhenProgramExists_ShouldMarkAsExpired() {
        // Given
        UUID programId = testProgram.getProgramId();
        when(fundingProgramRepository.findById(programId))
            .thenReturn(Optional.of(testProgram));
        when(fundingProgramRepository.save(any(FundingProgram.class)))
            .thenReturn(testProgram);

        // When
        FundingProgram result = fundingProgramService.markAsExpired(programId);

        // Then
        assertThat(result.getStatus()).isEqualTo(ProgramStatus.EXPIRED);
        assertThat(result.getIsActive()).isFalse();
        verify(fundingProgramRepository).save(testProgram);
    }

    @Test
    void markExpiredPrograms_ShouldUpdateExpiredPrograms() {
        // Given
        List<FundingProgram> expiredPrograms = List.of(testProgram);
        when(fundingProgramRepository.findExpiredPrograms(any(LocalDateTime.class)))
            .thenReturn(expiredPrograms);
        when(fundingProgramRepository.saveAll(any()))
            .thenReturn(expiredPrograms);

        // When
        int result = fundingProgramService.markExpiredPrograms();

        // Then
        assertThat(result).isEqualTo(1);
        assertThat(testProgram.getStatus()).isEqualTo(ProgramStatus.EXPIRED);
        assertThat(testProgram.getIsActive()).isFalse();
        verify(fundingProgramRepository).saveAll(expiredPrograms);
    }

    @Test
    void deactivate_WhenProgramExists_ShouldDeactivate() {
        // Given
        UUID programId = testProgram.getProgramId();
        when(fundingProgramRepository.findById(programId))
            .thenReturn(Optional.of(testProgram));
        when(fundingProgramRepository.save(any(FundingProgram.class)))
            .thenReturn(testProgram);

        // When
        FundingProgram result = fundingProgramService.deactivate(programId);

        // Then
        assertThat(result.getIsActive()).isFalse();
        verify(fundingProgramRepository).save(testProgram);
    }

    // ============================================================================
    // READ Operations Tests
    // ============================================================================

    @Test
    void programExists_WhenExists_ShouldReturnTrue() {
        // Given
        when(fundingProgramRepository.existsByProgramUrl("https://test.org/program"))
            .thenReturn(true);

        // When
        boolean result = fundingProgramService.programExists("https://test.org/program");

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void findByUrl_WhenExists_ShouldReturnProgram() {
        // Given
        when(fundingProgramRepository.findByProgramUrl("https://test.org/program"))
            .thenReturn(Optional.of(testProgram));

        // When
        Optional<FundingProgram> result = fundingProgramService.findByUrl(
            "https://test.org/program");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(testProgram);
    }

    @Test
    void findById_WhenExists_ShouldReturnProgram() {
        // Given
        UUID programId = testProgram.getProgramId();
        when(fundingProgramRepository.findById(programId))
            .thenReturn(Optional.of(testProgram));

        // When
        Optional<FundingProgram> result = fundingProgramService.findById(programId);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(testProgram);
    }

    @Test
    void getProgramsByOrganization_ShouldReturnOrgPrograms() {
        // Given
        List<FundingProgram> programs = List.of(testProgram);
        when(fundingProgramRepository.findByOrganizationId(testOrganizationId))
            .thenReturn(programs);

        // When
        List<FundingProgram> result = fundingProgramService.getProgramsByOrganization(
            testOrganizationId);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result).containsExactly(testProgram);
    }

    @Test
    void getProgramsByDomain_ShouldReturnDomainPrograms() {
        // Given
        List<FundingProgram> programs = List.of(testProgram);
        when(fundingProgramRepository.findByDomain("test.org"))
            .thenReturn(programs);

        // When
        List<FundingProgram> result = fundingProgramService.getProgramsByDomain("test.org");

        // Then
        assertThat(result).hasSize(1);
        assertThat(result).containsExactly(testProgram);
    }

    @Test
    void getProgramsByStatus_ShouldReturnStatusPrograms() {
        // Given
        List<FundingProgram> programs = List.of(testProgram);
        when(fundingProgramRepository.findByStatus(ProgramStatus.ACTIVE))
            .thenReturn(programs);

        // When
        List<FundingProgram> result = fundingProgramService.getProgramsByStatus(
            ProgramStatus.ACTIVE);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result).containsExactly(testProgram);
    }

    @Test
    void getActivePrograms_ShouldReturnActivePrograms() {
        // Given
        List<FundingProgram> programs = List.of(testProgram);
        when(fundingProgramRepository.findByIsActive(true))
            .thenReturn(programs);

        // When
        List<FundingProgram> result = fundingProgramService.getActivePrograms();

        // Then
        assertThat(result).hasSize(1);
        assertThat(result).containsExactly(testProgram);
    }

    @Test
    void getValidFundingOpportunities_ShouldReturnValidPrograms() {
        // Given
        List<FundingProgram> programs = List.of(testProgram);
        when(fundingProgramRepository.findByIsValidFundingOpportunity(true))
            .thenReturn(programs);

        // When
        List<FundingProgram> result = fundingProgramService.getValidFundingOpportunities();

        // Then
        assertThat(result).hasSize(1);
        assertThat(result).containsExactly(testProgram);
    }

    @Test
    void getProgramsWithUpcomingDeadlines_ShouldReturnUpcomingPrograms() {
        // Given
        List<FundingProgram> programs = List.of(testProgram);
        when(fundingProgramRepository.findProgramsWithUpcomingDeadlines(
            any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(programs);

        // When
        List<FundingProgram> result = fundingProgramService.getProgramsWithUpcomingDeadlines(30);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result).containsExactly(testProgram);
    }

    @Test
    void getHighConfidencePrograms_ShouldReturnHighConfidencePrograms() {
        // Given
        BigDecimal minConfidence = new BigDecimal("0.80");
        int limit = 10;
        List<FundingProgram> programs = List.of(testProgram);
        when(fundingProgramRepository.findHighConfidencePrograms(
            minConfidence, PageRequest.of(0, limit)))
            .thenReturn(programs);

        // When
        List<FundingProgram> result = fundingProgramService.getHighConfidencePrograms(
            minConfidence, limit);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result).containsExactly(testProgram);
    }

    @Test
    void getRecurringPrograms_ShouldReturnRecurringPrograms() {
        // Given
        List<FundingProgram> programs = List.of(testProgram);
        when(fundingProgramRepository.findByIsRecurring(true))
            .thenReturn(programs);

        // When
        List<FundingProgram> result = fundingProgramService.getRecurringPrograms();

        // Then
        assertThat(result).hasSize(1);
        assertThat(result).containsExactly(testProgram);
    }

    @Test
    void searchPrograms_ShouldReturnMatchingPrograms() {
        // Given
        String pattern = "%Test%";
        List<FundingProgram> programs = List.of(testProgram);
        when(fundingProgramRepository.searchByProgramName(pattern))
            .thenReturn(programs);

        // When
        List<FundingProgram> result = fundingProgramService.searchPrograms(pattern);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result).containsExactly(testProgram);
    }

    @Test
    void countByStatus_ShouldReturnCount() {
        // Given
        when(fundingProgramRepository.countByStatus(ProgramStatus.ACTIVE))
            .thenReturn(42L);

        // When
        long result = fundingProgramService.countByStatus(ProgramStatus.ACTIVE);

        // Then
        assertThat(result).isEqualTo(42L);
    }

    @Test
    void countActiveByOrganization_ShouldReturnCount() {
        // Given
        when(fundingProgramRepository.countActiveByOrganization(testOrganizationId))
            .thenReturn(10L);

        // When
        long result = fundingProgramService.countActiveByOrganization(testOrganizationId);

        // Then
        assertThat(result).isEqualTo(10L);
    }
}
