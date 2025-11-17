package com.northstar.funding.rest.dto;

import com.northstar.funding.domain.FundingSourceCandidate;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Maps between FundingSourceCandidate domain entity and CandidateDTO.
 *
 * Conversion rules:
 * - UUID → String via toString()
 * - BigDecimal → String via toString()
 * - Enum → String via name()
 * - LocalDateTime → String via ISO_LOCAL_DATE_TIME formatter
 */
@Service
public class CandidateDTOMapper {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    /**
     * Convert domain entity to DTO for API response.
     */
    public CandidateDTO toDTO(FundingSourceCandidate entity) {
        return new CandidateDTO(
            entity.getCandidateId().toString(),
            entity.getSourceUrl(),
            entity.getMetadataTitle(),
            entity.getConfidenceScore().toString(),
            entity.getStatus().name(),
            entity.getSearchEngineSource() != null ? entity.getSearchEngineSource().name() : null,
            entity.getDiscoveredAt().format(FORMATTER)
        );
    }

    /**
     * Convert list of domain entities to DTOs.
     */
    public List<CandidateDTO> toDTOs(List<FundingSourceCandidate> entities) {
        return entities.stream()
            .map(this::toDTO)
            .toList();
    }

    // Note: No toDomain() method - this feature only reads candidates (no create/update)
}
