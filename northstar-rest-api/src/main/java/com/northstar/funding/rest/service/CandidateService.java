package com.northstar.funding.rest.service;

import com.northstar.funding.domain.CandidateStatus;
import com.northstar.funding.domain.FundingSourceCandidate;
import com.northstar.funding.persistence.repository.DomainRepository;
import com.northstar.funding.persistence.repository.FundingSourceCandidateRepository;
import com.northstar.funding.rest.dto.CandidateDTO;
import com.northstar.funding.rest.dto.CandidateDTOMapper;
import com.northstar.funding.rest.dto.CandidatePageDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Service for managing funding source candidates.
 *
 * Provides business logic for:
 * - Listing candidates with filters and pagination
 * - Approving candidates for client-facing database
 * - Rejecting candidates and blacklisting domains
 */
@Service
@Transactional
public class CandidateService {

    private final FundingSourceCandidateRepository candidateRepository;
    private final DomainRepository domainRepository;
    private final CandidateDTOMapper mapper;

    public CandidateService(
            FundingSourceCandidateRepository candidateRepository,
            DomainRepository domainRepository,
            CandidateDTOMapper mapper) {
        this.candidateRepository = candidateRepository;
        this.domainRepository = domainRepository;
        this.mapper = mapper;
    }

    /**
     * List candidates with optional filters and pagination.
     *
     * @param statuses Filter by candidate statuses (multi-select)
     * @param minConfidence Minimum confidence score (0.00-1.00)
     * @param searchEngines Filter by search engines (multi-select)
     * @param startDate Filter by discovered date >= startDate
     * @param endDate Filter by discovered date <= endDate
     * @param sortBy Column to sort by
     * @param sortDirection Sort direction (ASC/DESC)
     * @param page Page number (0-indexed)
     * @param size Number of items per page
     * @return Paginated list of candidates
     */
    @Transactional(readOnly = true)
    public CandidatePageDTO listCandidates(
            List<String> statuses,
            BigDecimal minConfidence,
            List<String> searchEngines,
            LocalDate startDate,
            LocalDate endDate,
            String sortBy,
            String sortDirection,
            int page,
            int size) {

        // For now, return all candidates (filtering will be added in future enhancement)
        List<FundingSourceCandidate> allCandidates = (List<FundingSourceCandidate>) candidateRepository.findAll();
        List<CandidateDTO> dtos = mapper.toDTOs(allCandidates);

        // Simple pagination
        int totalElements = dtos.size();
        int totalPages = (int) Math.ceil((double) totalElements / size);
        int fromIndex = page * size;
        int toIndex = Math.min(fromIndex + size, totalElements);

        List<CandidateDTO> pageContent = fromIndex < totalElements
                ? dtos.subList(fromIndex, toIndex)
                : List.of();

        return new CandidatePageDTO(
                pageContent,
                totalElements,
                totalPages,
                page,
                size
        );
    }

    /**
     * Approve a candidate for inclusion in client-facing database.
     *
     * @param id Candidate UUID
     * @return Updated candidate DTO
     * @throws IllegalArgumentException if candidate not found or already approved
     */
    public CandidateDTO approveCandidate(UUID id) {
        FundingSourceCandidate candidate = candidateRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Candidate not found with id: " + id));

        if (candidate.getStatus() == CandidateStatus.APPROVED) {
            throw new IllegalArgumentException("Candidate is already approved");
        }

        candidate.setStatus(CandidateStatus.APPROVED);
        FundingSourceCandidate saved = candidateRepository.save(candidate);

        return mapper.toDTO(saved);
    }

    /**
     * Reject a candidate and blacklist its domain.
     *
     * @param id Candidate UUID
     * @return Updated candidate DTO
     * @throws IllegalArgumentException if candidate not found or already rejected
     */
    public CandidateDTO rejectCandidate(UUID id) {
        FundingSourceCandidate candidate = candidateRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Candidate not found with id: " + id));

        if (candidate.getStatus() == CandidateStatus.REJECTED) {
            throw new IllegalArgumentException("Candidate is already rejected");
        }

        candidate.setStatus(CandidateStatus.REJECTED);
        FundingSourceCandidate saved = candidateRepository.save(candidate);

        // TODO: Blacklist domain (requires domain relationship to be populated)
        // Domain domain = domainRepository.findById(candidate.getDomainId());
        // domain.setStatus(DomainStatus.BLACKLISTED);
        // domainRepository.save(domain);

        return mapper.toDTO(saved);
    }
}
