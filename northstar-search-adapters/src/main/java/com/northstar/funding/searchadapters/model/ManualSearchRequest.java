package com.northstar.funding.searchadapters.model;

import com.northstar.funding.domain.FundingSearchCategory;
import com.northstar.funding.domain.SearchEngineType;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Request model for manual search execution.
 *
 * <p>Used to trigger searches for specific categories and engines,
 * typically for testing or manual discovery outside the nightly workflow.
 *
 * <p>Example usage:
 * <pre>{@code
 * ManualSearchRequest request = ManualSearchRequest.builder()
 *     .categories(List.of(
 *         FundingSearchCategory.INDIVIDUAL_SCHOLARSHIPS,
 *         FundingSearchCategory.STEM_EDUCATION))
 *     .engines(List.of(
 *         SearchEngineType.TAVILY,
 *         SearchEngineType.SEARXNG))
 *     .maxResultsPerQuery(10)
 *     .geographicScope("Bulgaria, Eastern Europe, EU")
 *     .build();
 * }</pre>
 */
@Data
@Builder
public class ManualSearchRequest {

    /**
     * Funding categories to search for.
     * Must not be empty.
     */
    private List<FundingSearchCategory> categories;

    /**
     * Search engines to use.
     * Must not be empty.
     */
    private List<SearchEngineType> engines;

    /**
     * Maximum results to retrieve per query from each engine.
     * Default: 10
     * Range: 1-50
     */
    @Builder.Default
    private int maxResultsPerQuery = 10;

    /**
     * Geographic scope for searches.
     * Example: "Bulgaria, Eastern Europe, EU"
     * Used by query generation to focus on relevant regions.
     */
    @Builder.Default
    private String geographicScope = "Bulgaria, Eastern Europe, EU";
}
