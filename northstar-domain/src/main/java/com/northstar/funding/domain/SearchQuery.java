package com.northstar.funding.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;

/**
 * Domain entity representing a search query in the query library.
 *
 * <p>Queries can be either hardcoded or AI-generated. This entity supports both use cases:
 * <ul>
 *   <li>Hardcoded queries: Used for nightly scheduled searches</li>
 *   <li>AI-generated queries: Created by the query generation service</li>
 * </ul>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("search_queries")
public class SearchQuery {

    @Id
    private Long id;

    private String queryText;
    private String dayOfWeek;
    private Set<String> tags;
    private Set<String> targetEngines;
    private Integer expectedResults;
    private Boolean enabled;
    private Instant createdAt;
    private Instant updatedAt;

    // AI Generation Fields (Feature 004)
    private String generationMethod; // AI_GENERATED or HARDCODED
    private String aiModelUsed;
    private String queryTemplateId;
    private Integer semanticClusterId;
    private Long generationSessionId;
    private LocalDate generationDate;
}
