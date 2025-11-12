package com.northstar.funding.rest.dto;

import com.northstar.funding.domain.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.Set;

/**
 * Request model for search execution endpoint.
 *
 * <p>Contains all parameters needed to generate search queries and execute the workflow:
 * <ul>
 *   <li>Categories: Funding type, mechanism, scale, etc. (for query generation)</li>
 *   <li>Geographic scope: Target regions for search</li>
 *   <li>Query language: Language for search queries</li>
 *   <li>Max results per query: Limit results from each search engine</li>
 * </ul>
 */
public record SearchExecutionRequest(
        @NotEmpty(message = "At least one funding source type is required")
        Set<FundingSourceType> fundingSourceTypes,

        @NotEmpty(message = "At least one funding mechanism is required")
        Set<FundingMechanism> fundingMechanisms,

        @NotNull(message = "Project scale is required")
        ProjectScale projectScale,

        @NotEmpty(message = "At least one beneficiary population is required")
        Set<BeneficiaryPopulation> beneficiaryPopulations,

        @NotEmpty(message = "At least one recipient organization type is required")
        Set<RecipientOrganizationType> recipientOrganizationTypes,

        @NotEmpty(message = "At least one geographic scope is required")
        Set<String> geographicScope,

        @NotNull(message = "Query language is required")
        QueryLanguage queryLanguage,

        @Min(value = 10, message = "Max results must be at least 10")
        @Max(value = 100, message = "Max results must not exceed 100")
        int maxResultsPerQuery
) {
}
