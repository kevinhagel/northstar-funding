package com.northstar.funding.persistence.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.northstar.funding.domain.FundingProgram;
import com.northstar.funding.domain.ProgramStatus;
import com.northstar.funding.persistence.repository.FundingProgramRepository;


/**
 * Service layer for FundingProgram entity operations.
 *
 * Provides business logic and transaction management for funding program
 * management, URL deduplication, and deadline tracking.
 *
 * This is the public API for external modules to interact with FundingProgram persistence.
 */
@Service
@Transactional
public class FundingProgramService {

    private final FundingProgramRepository fundingProgramRepository;

    public FundingProgramService(FundingProgramRepository fundingProgramRepository) {
        this.fundingProgramRepository = fundingProgramRepository;
    }

    // ============================================================================
    // CREATE & UPDATE Operations
    // ============================================================================

    /**
     * Register a new funding program if URL doesn't already exist.
     *
     * @param organizationId the organization offering this program
     * @param domain the domain name
     * @param programName the program name
     * @param programUrl the program URL (unique)
     * @param description optional description
     * @return the existing or newly created FundingProgram
     */
    public FundingProgram registerProgram(UUID organizationId, String domain,
                                          String programName, String programUrl,
                                          String description) {

        return fundingProgramRepository.findByProgramUrl(programUrl)
            .orElseGet(() -> {
                FundingProgram program = FundingProgram.builder()
                    .organizationId(organizationId)
                    .domain(domain)
                    .programName(programName)
                    .programUrl(programUrl)
                    .description(description)
                    .discoveredAt(LocalDateTime.now())
                    .isActive(true)
                    .build();

                FundingProgram saved = fundingProgramRepository.save(program);
                return saved;
            });
    }

    /**
     * Update program status.
     *
     * @param programId the program ID
     * @param status the new status
     * @return updated FundingProgram
     */
    public FundingProgram updateStatus(UUID programId, ProgramStatus status) {

        FundingProgram program = fundingProgramRepository.findById(programId)
            .orElseThrow(() -> new IllegalArgumentException("Program not found: " + programId));

        program.setStatus(status);
        program.setLastRefreshedAt(LocalDateTime.now());

        return fundingProgramRepository.save(program);
    }

    /**
     * Mark program as validated funding opportunity.
     *
     * @param programId the program ID
     * @param confidence AI confidence score (0.00-1.00)
     * @return updated FundingProgram
     */
    public FundingProgram markAsValidFundingOpportunity(UUID programId, BigDecimal confidence) {
        FundingProgram program = fundingProgramRepository.findById(programId)
            .orElseThrow(() -> new IllegalArgumentException("Program not found: " + programId));

        program.setIsValidFundingOpportunity(true);
        program.setProgramConfidence(confidence);
        program.setLastRefreshedAt(LocalDateTime.now());

        return fundingProgramRepository.save(program);
    }

    /**
     * Update program deadline.
     *
     * @param programId the program ID
     * @param deadline the application deadline
     * @return updated FundingProgram
     */
    public FundingProgram updateDeadline(UUID programId, LocalDateTime deadline) {

        FundingProgram program = fundingProgramRepository.findById(programId)
            .orElseThrow(() -> new IllegalArgumentException("Program not found: " + programId));

        program.setApplicationDeadline(deadline);
        program.setLastRefreshedAt(LocalDateTime.now());

        return fundingProgramRepository.save(program);
    }

    /**
     * Mark program as expired.
     *
     * @param programId the program ID
     * @return updated FundingProgram
     */
    public FundingProgram markAsExpired(UUID programId) {

        FundingProgram program = fundingProgramRepository.findById(programId)
            .orElseThrow(() -> new IllegalArgumentException("Program not found: " + programId));

        program.setStatus(ProgramStatus.EXPIRED);
        program.setIsActive(false);

        return fundingProgramRepository.save(program);
    }

    /**
     * Batch mark programs as expired by deadline.
     *
     * @return number of programs marked as expired
     */
    public int markExpiredPrograms() {

        List<FundingProgram> expired = fundingProgramRepository.findExpiredPrograms(LocalDateTime.now());

        expired.forEach(program -> {
            program.setStatus(ProgramStatus.EXPIRED);
            program.setIsActive(false);
        });

        fundingProgramRepository.saveAll(expired);

        return expired.size();
    }

    /**
     * Deactivate a program.
     *
     * @param programId the program ID
     * @return updated FundingProgram
     */
    public FundingProgram deactivate(UUID programId) {

        FundingProgram program = fundingProgramRepository.findById(programId)
            .orElseThrow(() -> new IllegalArgumentException("Program not found: " + programId));

        program.setIsActive(false);
        return fundingProgramRepository.save(program);
    }

    // ============================================================================
    // READ Operations
    // ============================================================================

    /**
     * Check if a program URL already exists.
     *
     * @param programUrl the program URL
     * @return true if exists
     */
    @Transactional(readOnly = true)
    public boolean programExists(String programUrl) {
        return fundingProgramRepository.existsByProgramUrl(programUrl);
    }

    /**
     * Find program by URL.
     *
     * @param programUrl the program URL
     * @return Optional of FundingProgram
     */
    @Transactional(readOnly = true)
    public Optional<FundingProgram> findByUrl(String programUrl) {
        return fundingProgramRepository.findByProgramUrl(programUrl);
    }

    /**
     * Find program by ID.
     *
     * @param programId the program ID
     * @return Optional of FundingProgram
     */
    @Transactional(readOnly = true)
    public Optional<FundingProgram> findById(UUID programId) {
        return fundingProgramRepository.findById(programId);
    }

    /**
     * Get all programs for an organization.
     *
     * @param organizationId the organization ID
     * @return list of programs
     */
    @Transactional(readOnly = true)
    public List<FundingProgram> getProgramsByOrganization(UUID organizationId) {
        return fundingProgramRepository.findByOrganizationId(organizationId);
    }

    /**
     * Get all programs for a domain.
     *
     * @param domain the domain name
     * @return list of programs
     */
    @Transactional(readOnly = true)
    public List<FundingProgram> getProgramsByDomain(String domain) {
        return fundingProgramRepository.findByDomain(domain);
    }

    /**
     * Get programs by status.
     *
     * @param status the program status
     * @return list of programs
     */
    @Transactional(readOnly = true)
    public List<FundingProgram> getProgramsByStatus(ProgramStatus status) {
        return fundingProgramRepository.findByStatus(status);
    }

    /**
     * Get active programs.
     *
     * @return list of active programs
     */
    @Transactional(readOnly = true)
    public List<FundingProgram> getActivePrograms() {
        return fundingProgramRepository.findByIsActive(true);
    }

    /**
     * Get validated funding opportunities.
     *
     * @return list of valid funding opportunities
     */
    @Transactional(readOnly = true)
    public List<FundingProgram> getValidFundingOpportunities() {
        return fundingProgramRepository.findByIsValidFundingOpportunity(true);
    }

    /**
     * Get programs with upcoming deadlines.
     *
     * @param daysAhead number of days to look ahead
     * @return list of programs with upcoming deadlines
     */
    @Transactional(readOnly = true)
    public List<FundingProgram> getProgramsWithUpcomingDeadlines(int daysAhead) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime future = now.plusDays(daysAhead);

        return fundingProgramRepository.findProgramsWithUpcomingDeadlines(now, future);
    }

    /**
     * Get high-confidence programs.
     *
     * @param minConfidence minimum confidence threshold
     * @param limit max number of results
     * @return list of high-confidence programs
     */
    @Transactional(readOnly = true)
    public List<FundingProgram> getHighConfidencePrograms(BigDecimal minConfidence, int limit) {
        return fundingProgramRepository.findHighConfidencePrograms(
            minConfidence,
            PageRequest.of(0, limit)
        );
    }

    /**
     * Get recurring programs.
     *
     * @return list of recurring programs
     */
    @Transactional(readOnly = true)
    public List<FundingProgram> getRecurringPrograms() {
        return fundingProgramRepository.findByIsRecurring(true);
    }

    /**
     * Search programs by name pattern (ILIKE).
     *
     * @param pattern search pattern
     * @return list of matching programs
     */
    @Transactional(readOnly = true)
    public List<FundingProgram> searchPrograms(String pattern) {
        return fundingProgramRepository.searchByProgramName(pattern);
    }

    /**
     * Count programs by status.
     *
     * @param status the program status
     * @return count
     */
    @Transactional(readOnly = true)
    public long countByStatus(ProgramStatus status) {
        return fundingProgramRepository.countByStatus(status);
    }

    /**
     * Count active programs for an organization.
     *
     * @param organizationId the organization ID
     * @return count of active programs
     */
    @Transactional(readOnly = true)
    public long countActiveByOrganization(UUID organizationId) {
        return fundingProgramRepository.countActiveByOrganization(organizationId);
    }
}
