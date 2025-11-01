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

import com.northstar.funding.domain.Organization;
import com.northstar.funding.persistence.repository.OrganizationRepository;

/**
 * Unit tests for OrganizationService using Mockito.
 */
@ExtendWith(MockitoExtension.class)
class OrganizationServiceTest {

    @Mock
    private OrganizationRepository organizationRepository;

    @InjectMocks
    private OrganizationService organizationService;

    private Organization testOrganization;
    private UUID testSessionId;

    @BeforeEach
    void setUp() {
        testSessionId = UUID.randomUUID();
        testOrganization = Organization.builder()
            .organizationId(UUID.randomUUID())
            .name("Test Organization")
            .domain("test.org")
            .discoverySessionId(testSessionId)
            .discoveredAt(LocalDateTime.now())
            .isActive(true)
            .programCount(0)
            .build();
    }

    // ============================================================================
    // CREATE & UPDATE Operations Tests
    // ============================================================================

    @Test
    void registerOrganization_WhenNew_ShouldCreateOrganization() {
        // Given
        when(organizationRepository.findByDomain("test.org"))
            .thenReturn(Optional.empty());
        when(organizationRepository.save(any(Organization.class)))
            .thenReturn(testOrganization);

        // When
        Organization result = organizationService.registerOrganization(
            "Test Organization", "test.org", testSessionId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Test Organization");
        assertThat(result.getDomain()).isEqualTo("test.org");
        verify(organizationRepository).findByDomain("test.org");
        verify(organizationRepository).save(any(Organization.class));
    }

    @Test
    void registerOrganization_WhenExists_ShouldReturnExisting() {
        // Given
        when(organizationRepository.findByDomain("test.org"))
            .thenReturn(Optional.of(testOrganization));

        // When
        Organization result = organizationService.registerOrganization(
            "Test Organization", "test.org", testSessionId);

        // Then
        assertThat(result).isEqualTo(testOrganization);
        verify(organizationRepository).findByDomain("test.org");
        verify(organizationRepository, never()).save(any(Organization.class));
    }

    @Test
    void updateMetadata_WhenOrganizationExists_ShouldUpdateMetadata() {
        // Given
        UUID orgId = testOrganization.getOrganizationId();
        when(organizationRepository.findById(orgId))
            .thenReturn(Optional.of(testOrganization));
        when(organizationRepository.save(any(Organization.class)))
            .thenReturn(testOrganization);

        String mission = "Support education";
        String geo = "Bulgaria";
        String homepage = "https://test.org";

        // When
        Organization result = organizationService.updateMetadata(orgId, mission, geo, homepage);

        // Then
        assertThat(result.getMission()).isEqualTo(mission);
        assertThat(result.getGeographicFocus()).isEqualTo(geo);
        assertThat(result.getHomepageUrl()).isEqualTo(homepage);
        assertThat(result.getLastRefreshedAt()).isNotNull();
        verify(organizationRepository).save(testOrganization);
    }

    @Test
    void updateMetadata_WhenOrganizationNotFound_ShouldThrowException() {
        // Given
        UUID orgId = UUID.randomUUID();
        when(organizationRepository.findById(orgId))
            .thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> organizationService.updateMetadata(
            orgId, "mission", "geo", "url"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Organization not found");
        verify(organizationRepository, never()).save(any(Organization.class));
    }

    @Test
    void markAsValidFundingSource_WhenOrganizationExists_ShouldMarkAsValid() {
        // Given
        UUID orgId = testOrganization.getOrganizationId();
        BigDecimal confidence = new BigDecimal("0.92");
        when(organizationRepository.findById(orgId))
            .thenReturn(Optional.of(testOrganization));
        when(organizationRepository.save(any(Organization.class)))
            .thenReturn(testOrganization);

        // When
        Organization result = organizationService.markAsValidFundingSource(orgId, confidence);

        // Then
        assertThat(result.getIsValidFundingSource()).isTrue();
        assertThat(result.getOrganizationConfidence()).isEqualTo(confidence);
        assertThat(result.getLastRefreshedAt()).isNotNull();
        verify(organizationRepository).save(testOrganization);
    }

    @Test
    void incrementProgramCount_WhenOrganizationExists_ShouldIncrementCount() {
        // Given
        UUID orgId = testOrganization.getOrganizationId();
        testOrganization.setProgramCount(5);
        when(organizationRepository.findById(orgId))
            .thenReturn(Optional.of(testOrganization));
        when(organizationRepository.save(any(Organization.class)))
            .thenReturn(testOrganization);

        // When
        organizationService.incrementProgramCount(orgId);

        // Then
        assertThat(testOrganization.getProgramCount()).isEqualTo(6);
        verify(organizationRepository).save(testOrganization);
    }

    @Test
    void incrementProgramCount_WhenCountIsNull_ShouldSetToOne() {
        // Given
        UUID orgId = testOrganization.getOrganizationId();
        testOrganization.setProgramCount(null);
        when(organizationRepository.findById(orgId))
            .thenReturn(Optional.of(testOrganization));
        when(organizationRepository.save(any(Organization.class)))
            .thenReturn(testOrganization);

        // When
        organizationService.incrementProgramCount(orgId);

        // Then
        assertThat(testOrganization.getProgramCount()).isEqualTo(1);
        verify(organizationRepository).save(testOrganization);
    }

    @Test
    void deactivate_WhenOrganizationExists_ShouldDeactivate() {
        // Given
        UUID orgId = testOrganization.getOrganizationId();
        when(organizationRepository.findById(orgId))
            .thenReturn(Optional.of(testOrganization));
        when(organizationRepository.save(any(Organization.class)))
            .thenReturn(testOrganization);

        // When
        Organization result = organizationService.deactivate(orgId);

        // Then
        assertThat(result.getIsActive()).isFalse();
        verify(organizationRepository).save(testOrganization);
    }

    // ============================================================================
    // READ Operations Tests
    // ============================================================================

    @Test
    void organizationExistsForDomain_WhenExists_ShouldReturnTrue() {
        // Given
        when(organizationRepository.existsByDomain("test.org"))
            .thenReturn(true);

        // When
        boolean result = organizationService.organizationExistsForDomain("test.org");

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void findByDomain_WhenExists_ShouldReturnOrganization() {
        // Given
        when(organizationRepository.findByDomain("test.org"))
            .thenReturn(Optional.of(testOrganization));

        // When
        Optional<Organization> result = organizationService.findByDomain("test.org");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(testOrganization);
    }

    @Test
    void findById_WhenExists_ShouldReturnOrganization() {
        // Given
        UUID orgId = testOrganization.getOrganizationId();
        when(organizationRepository.findById(orgId))
            .thenReturn(Optional.of(testOrganization));

        // When
        Optional<Organization> result = organizationService.findById(orgId);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(testOrganization);
    }

    @Test
    void getValidFundingSources_ShouldReturnValidSources() {
        // Given
        List<Organization> validSources = List.of(testOrganization);
        when(organizationRepository.findByIsValidFundingSource(true))
            .thenReturn(validSources);

        // When
        List<Organization> result = organizationService.getValidFundingSources();

        // Then
        assertThat(result).hasSize(1);
        assertThat(result).containsExactly(testOrganization);
    }

    @Test
    void getActiveOrganizations_ShouldReturnActiveOrgs() {
        // Given
        List<Organization> active = List.of(testOrganization);
        when(organizationRepository.findByIsActive(true))
            .thenReturn(active);

        // When
        List<Organization> result = organizationService.getActiveOrganizations();

        // Then
        assertThat(result).hasSize(1);
        assertThat(result).containsExactly(testOrganization);
    }

    @Test
    void getHighConfidenceOrganizations_ShouldReturnHighConfidenceOrgs() {
        // Given
        BigDecimal minConfidence = new BigDecimal("0.80");
        int limit = 10;
        List<Organization> highConfidence = List.of(testOrganization);
        when(organizationRepository.findHighConfidenceOrganizations(
            minConfidence, PageRequest.of(0, limit)))
            .thenReturn(highConfidence);

        // When
        List<Organization> result = organizationService.getHighConfidenceOrganizations(
            minConfidence, limit);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result).containsExactly(testOrganization);
    }

    @Test
    void getOrganizationsWithMultiplePrograms_ShouldReturnOrgsWithPrograms() {
        // Given
        int minPrograms = 5;
        List<Organization> withPrograms = List.of(testOrganization);
        when(organizationRepository.findOrganizationsWithMultiplePrograms(minPrograms))
            .thenReturn(withPrograms);

        // When
        List<Organization> result = organizationService.getOrganizationsWithMultiplePrograms(
            minPrograms);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result).containsExactly(testOrganization);
    }

    @Test
    void getOrganizationsNeedingRefresh_ShouldReturnOrgsNeedingRefresh() {
        // Given
        int daysThreshold = 30;
        List<Organization> needingRefresh = List.of(testOrganization);
        when(organizationRepository.findOrganizationsNeedingRefresh(any(LocalDateTime.class)))
            .thenReturn(needingRefresh);

        // When
        List<Organization> result = organizationService.getOrganizationsNeedingRefresh(
            daysThreshold);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result).containsExactly(testOrganization);
    }

    @Test
    void getOrganizationsBySession_ShouldReturnSessionOrgs() {
        // Given
        List<Organization> sessionOrgs = List.of(testOrganization);
        when(organizationRepository.findByDiscoverySessionId(testSessionId))
            .thenReturn(sessionOrgs);

        // When
        List<Organization> result = organizationService.getOrganizationsBySession(testSessionId);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result).containsExactly(testOrganization);
    }

    @Test
    void searchOrganizations_ShouldReturnMatchingOrgs() {
        // Given
        String pattern = "%Test%";
        List<Organization> matches = List.of(testOrganization);
        when(organizationRepository.searchByName(pattern))
            .thenReturn(matches);

        // When
        List<Organization> result = organizationService.searchOrganizations(pattern);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result).containsExactly(testOrganization);
    }

    @Test
    void countByValidationStatus_ShouldReturnCount() {
        // Given
        when(organizationRepository.countByValidationStatus(true))
            .thenReturn(42L);

        // When
        long result = organizationService.countByValidationStatus(true);

        // Then
        assertThat(result).isEqualTo(42L);
    }
}
