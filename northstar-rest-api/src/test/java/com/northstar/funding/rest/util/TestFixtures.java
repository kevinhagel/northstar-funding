package com.northstar.funding.rest.util;

import com.northstar.funding.domain.*;
import com.northstar.funding.rest.dto.SearchExecutionRequest;

import java.util.Set;

/**
 * Test fixture factory for creating standard test data.
 * Provides consistent, reusable test data for integration tests.
 */
public class TestFixtures {

    /**
     * Standard valid search request for testing successful workflows.
     * Uses Bulgaria/Eastern Europe focus with educational funding.
     */
    public static SearchExecutionRequest validSearchRequest() {
        return new SearchExecutionRequest(
            Set.of(FundingSourceType.GOVERNMENT_EU, FundingSourceType.GOVERNMENT_NATIONAL),
            Set.of(FundingMechanism.GRANT, FundingMechanism.SCHOLARSHIP),
            ProjectScale.SMALL,
            Set.of(BeneficiaryPopulation.EDUCATORS_TEACHERS),
            Set.of(RecipientOrganizationType.UNIVERSITY_PUBLIC),
            Set.of("Bulgaria", "Eastern Europe"),
            QueryLanguage.ENGLISH,
            25
        );
    }

    /**
     * Invalid search request with empty fundingSourceTypes.
     * Should trigger validation error: "At least one funding source type is required"
     */
    public static SearchExecutionRequest invalidSearchRequest_MissingFundingTypes() {
        return new SearchExecutionRequest(
            Set.of(), // EMPTY - validation error
            Set.of(FundingMechanism.GRANT),
            ProjectScale.SMALL,
            Set.of(BeneficiaryPopulation.EDUCATORS_TEACHERS),
            Set.of(RecipientOrganizationType.UNIVERSITY_PUBLIC),
            Set.of("Bulgaria"),
            QueryLanguage.ENGLISH,
            25
        );
    }

    /**
     * Invalid search request with maxResultsPerQuery below minimum (10).
     * Should trigger validation error: "Max results must be between 10 and 100"
     */
    public static SearchExecutionRequest invalidSearchRequest_LowMaxResults() {
        return new SearchExecutionRequest(
            Set.of(FundingSourceType.GOVERNMENT_EU),
            Set.of(FundingMechanism.GRANT),
            ProjectScale.SMALL,
            Set.of(BeneficiaryPopulation.EDUCATORS_TEACHERS),
            Set.of(RecipientOrganizationType.UNIVERSITY_PUBLIC),
            Set.of("Bulgaria"),
            QueryLanguage.ENGLISH,
            5  // TOO LOW - min is 10
        );
    }

    /**
     * Invalid search request with maxResultsPerQuery above maximum (100).
     * Should trigger validation error: "Max results must be between 10 and 100"
     */
    public static SearchExecutionRequest invalidSearchRequest_HighMaxResults() {
        return new SearchExecutionRequest(
            Set.of(FundingSourceType.GOVERNMENT_EU),
            Set.of(FundingMechanism.GRANT),
            ProjectScale.SMALL,
            Set.of(BeneficiaryPopulation.EDUCATORS_TEACHERS),
            Set.of(RecipientOrganizationType.UNIVERSITY_PUBLIC),
            Set.of("Bulgaria"),
            QueryLanguage.ENGLISH,
            150  // TOO HIGH - max is 100
        );
    }
}
