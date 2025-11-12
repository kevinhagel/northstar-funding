package com.northstar.funding.rest.controller;

import com.northstar.funding.kafka.events.SearchRequestEvent;
import com.northstar.funding.persistence.service.DiscoverySessionService;
import com.northstar.funding.querygeneration.service.QueryGenerationService;
import com.northstar.funding.rest.dto.SearchExecutionRequest;
import com.northstar.funding.rest.dto.SearchExecutionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * REST controller for search workflow triggers.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Accept search execution requests via HTTP POST</li>
 *   <li>Generate AI-powered search queries</li>
 *   <li>Publish SearchRequestEvent to Kafka for workflow processing</li>
 *   <li>Return session tracking information</li>
 * </ul>
 *
 * <p>The actual search execution happens asynchronously via Kafka consumers.
 * Use the returned sessionId to track progress in the database.
 */
@RestController
@RequestMapping("/api/search")
@Tag(name = "Search", description = "Search execution and management")
public class SearchController {

    private static final Logger log = LoggerFactory.getLogger(SearchController.class);

    private final QueryGenerationService queryGenerationService;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final DiscoverySessionService sessionService;

    public SearchController(
            QueryGenerationService queryGenerationService,
            KafkaTemplate<String, Object> kafkaTemplate,
            DiscoverySessionService sessionService
    ) {
        this.queryGenerationService = queryGenerationService;
        this.kafkaTemplate = kafkaTemplate;
        this.sessionService = sessionService;
    }

    /**
     * Execute search workflow.
     *
     * <p>Flow:
     * <ol>
     *   <li>Generate AI-powered search queries based on categories</li>
     *   <li>Create discovery session in database</li>
     *   <li>Publish SearchRequestEvent for each query to Kafka</li>
     *   <li>Return sessionId for tracking</li>
     * </ol>
     *
     * @param request Search execution parameters (categories, max results, etc.)
     * @return Search execution response with sessionId and status
     */
    @PostMapping("/execute")
    @Operation(
            summary = "Execute search workflow",
            description = """
                    Initiates an AI-powered funding search workflow.

                    1. Generates optimized search queries based on provided categories
                    2. Creates a discovery session for tracking
                    3. Publishes queries to Kafka for asynchronous processing
                    4. Returns sessionId to track progress

                    The actual search happens asynchronously via Kafka consumers.
                    """
    )
    public ResponseEntity<SearchExecutionResponse> executeSearch(
            @RequestBody @Valid SearchExecutionRequest request
    ) {
        log.info("📤 Search execution requested: fundingTypes={}, mechanisms={}, scale={}, language={}",
                request.fundingSourceTypes(),
                request.fundingMechanisms(),
                request.projectScale(),
                request.queryLanguage());

        // 1. Generate search queries using AI (for all search engines)
        UUID sessionId = UUID.randomUUID();
        List<String> queries = generateQueriesForAllEngines(request, sessionId);

        log.info("✅ Generated {} search queries across multiple engines", queries.size());

        // 2. Create discovery session
        var session = com.northstar.funding.domain.DiscoverySession.builder()
                .sessionId(sessionId)
                .status(com.northstar.funding.domain.SessionStatus.RUNNING)
                .sessionType(com.northstar.funding.domain.SessionType.MANUAL)
                .build();
        sessionService.createSession(session);

        // 3. Publish SearchRequestEvent for each query
        queries.forEach(query -> {
            SearchRequestEvent event = SearchRequestEvent.builder()
                    .sessionId(sessionId)
                    .query(query)
                    .maxResults(request.maxResultsPerQuery())
                    .build();

            kafkaTemplate.send("search-requests", event);
            log.debug("📨 Published SearchRequestEvent: sessionId={}, query=\"{}\"", sessionId, query);
        });

        log.info("🚀 Search workflow initiated: sessionId={}, queries={}", sessionId, queries.size());

        // 4. Return response
        return ResponseEntity.ok(SearchExecutionResponse.initiated(sessionId, queries.size()));
    }

    /**
     * Generate queries for all search engines by adapting REST request to query generation service.
     *
     * <p>This adapter method bridges the gap between the user-friendly REST API and the internal
     * query generation service. It generates queries for multiple search engines concurrently.
     *
     * @param request REST API request with user-provided criteria
     * @param sessionId Discovery session identifier
     * @return Combined list of queries from all search engines
     */
    private List<String> generateQueriesForAllEngines(SearchExecutionRequest request, UUID sessionId) {
        List<String> allQueries = new ArrayList<>();

        // Generate queries for each search engine
        // Note: Using SEARXNG (self-hosted), TAVILY (AI-optimized), SERPER (Google)
        // BRAVE excluded for now (requires API key setup)
        List<com.northstar.funding.domain.SearchEngineType> engines = List.of(
                com.northstar.funding.domain.SearchEngineType.SEARXNG,
                com.northstar.funding.domain.SearchEngineType.TAVILY,
                com.northstar.funding.domain.SearchEngineType.SERPER
        );

        for (com.northstar.funding.domain.SearchEngineType engine : engines) {
            com.northstar.funding.querygeneration.model.QueryGenerationRequest qgRequest =
                    buildQueryGenerationRequest(request, engine, sessionId);

            log.debug("🔍 Generating queries for engine: {}", engine);

            // Generate queries for this engine
            java.util.concurrent.CompletableFuture<com.northstar.funding.querygeneration.model.QueryGenerationResponse> future =
                    queryGenerationService.generateQueries(qgRequest);

            try {
                var response = future.join();
                allQueries.addAll(response.getQueries());
                log.debug("✅ Generated {} queries for {}", response.getQueries().size(), engine);
            } catch (Exception e) {
                log.error("❌ Failed to generate queries for {}: {}", engine, e.getMessage(), e);
                // Continue with other engines even if one fails
            }
        }

        return allQueries;
    }

    /**
     * Build QueryGenerationRequest from REST API request.
     *
     * <p>Maps user-friendly REST DTO to internal query generation service contract.
     *
     * @param request REST API request
     * @param engine Target search engine
     * @param sessionId Session identifier
     * @return QueryGenerationRequest for service
     */
    private com.northstar.funding.querygeneration.model.QueryGenerationRequest buildQueryGenerationRequest(
            SearchExecutionRequest request,
            com.northstar.funding.domain.SearchEngineType engine,
            UUID sessionId
    ) {
        return com.northstar.funding.querygeneration.model.QueryGenerationRequest.builder()
                .searchEngine(engine)
                .sessionId(sessionId)
                .maxQueries(3)  // Generate 3 queries per engine
                // Map funding categories from REST request parameters
                .categories(mapToFundingCategories(request))
                // Map geographic scope from free-form strings to enum
                .geographic(mapToGeographicScope(request.geographicScope()))
                // Map optional fields from REST request
                .projectScale(request.projectScale())
                .beneficiaries(request.beneficiaryPopulations())
                .userLanguage(request.queryLanguage())
                // Note: sourceType, mechanism, recipientType are Sets in REST request
                // but single values in QueryGenerationRequest. We'll use first element if present.
                .build();
    }

    /**
     * Map REST request parameters to FundingSearchCategory set.
     *
     * <p>Infers appropriate funding categories based on beneficiary populations,
     * recipient organization types, funding mechanisms, and project scale.
     *
     * @param request REST API request
     * @return Set of funding search categories
     */
    private Set<com.northstar.funding.domain.FundingSearchCategory> mapToFundingCategories(SearchExecutionRequest request) {
        Set<com.northstar.funding.domain.FundingSearchCategory> categories = new java.util.HashSet<>();

        // Map beneficiary populations to categories
        for (com.northstar.funding.domain.BeneficiaryPopulation beneficiary : request.beneficiaryPopulations()) {
            categories.addAll(mapBeneficiaryToCategories(beneficiary));
        }

        // Map recipient organization types to categories
        for (com.northstar.funding.domain.RecipientOrganizationType recipient : request.recipientOrganizationTypes()) {
            categories.addAll(mapRecipientToCategories(recipient));
        }

        // Map funding mechanisms to categories
        for (com.northstar.funding.domain.FundingMechanism mechanism : request.fundingMechanisms()) {
            categories.addAll(mapMechanismToCategories(mechanism));
        }

        // If no categories mapped, add broad categories as fallback
        if (categories.isEmpty()) {
            categories.add(com.northstar.funding.domain.FundingSearchCategory.PROGRAM_GRANTS);
            categories.add(com.northstar.funding.domain.FundingSearchCategory.INNOVATION_GRANTS);
        }

        return categories;
    }

    private Set<com.northstar.funding.domain.FundingSearchCategory> mapBeneficiaryToCategories(
            com.northstar.funding.domain.BeneficiaryPopulation beneficiary
    ) {
        return switch (beneficiary) {
            case EARLY_CHILDHOOD_0_5 -> Set.of(com.northstar.funding.domain.FundingSearchCategory.EARLY_CHILDHOOD_EDUCATION);
            case CHILDREN_AGES_4_12, ADOLESCENTS_AGES_13_18 -> Set.of(
                    com.northstar.funding.domain.FundingSearchCategory.PROGRAM_GRANTS,
                    com.northstar.funding.domain.FundingSearchCategory.AFTER_SCHOOL_PROGRAMS
            );
            case FIRST_GENERATION_STUDENTS -> Set.of(
                    com.northstar.funding.domain.FundingSearchCategory.INDIVIDUAL_SCHOLARSHIPS,
                    com.northstar.funding.domain.FundingSearchCategory.STUDENT_FINANCIAL_AID,
                    com.northstar.funding.domain.FundingSearchCategory.ACADEMIC_FELLOWSHIPS
            );
            case EDUCATORS_TEACHERS -> Set.of(
                    com.northstar.funding.domain.FundingSearchCategory.TEACHER_SCHOLARSHIPS,
                    com.northstar.funding.domain.FundingSearchCategory.TEACHER_DEVELOPMENT,
                    com.northstar.funding.domain.FundingSearchCategory.PROFESSIONAL_TRAINING
            );
            case ADULTS_LIFELONG_LEARNING -> Set.of(
                    com.northstar.funding.domain.FundingSearchCategory.ADULT_EDUCATION,
                    com.northstar.funding.domain.FundingSearchCategory.VOCATIONAL_TRAINING
            );
            case AT_RISK_YOUTH -> Set.of(
                    com.northstar.funding.domain.FundingSearchCategory.AFTER_SCHOOL_PROGRAMS,
                    com.northstar.funding.domain.FundingSearchCategory.SUMMER_PROGRAMS,
                    com.northstar.funding.domain.FundingSearchCategory.COMMUNITY_PARTNERSHIPS
            );
            case PEOPLE_WITH_DISABILITIES -> Set.of(
                    com.northstar.funding.domain.FundingSearchCategory.SPECIAL_NEEDS_EDUCATION
            );
            case RURAL_COMMUNITIES, LOW_INCOME_FAMILIES -> Set.of(
                    com.northstar.funding.domain.FundingSearchCategory.PROGRAM_GRANTS,
                    com.northstar.funding.domain.FundingSearchCategory.COMMUNITY_PARTNERSHIPS
            );
            case ETHNIC_MINORITIES, REFUGEES_IMMIGRANTS, GIRLS_WOMEN, LGBTQ_PLUS,
                 LANGUAGE_MINORITIES -> Set.of(
                    com.northstar.funding.domain.FundingSearchCategory.INDIVIDUAL_SCHOLARSHIPS,
                    com.northstar.funding.domain.FundingSearchCategory.PROGRAM_GRANTS
            );
            case VETERANS, ELDERLY, GENERAL_POPULATION -> Set.of(
                    com.northstar.funding.domain.FundingSearchCategory.ADULT_EDUCATION,
                    com.northstar.funding.domain.FundingSearchCategory.PROGRAM_GRANTS
            );
        };
    }

    private Set<com.northstar.funding.domain.FundingSearchCategory> mapRecipientToCategories(
            com.northstar.funding.domain.RecipientOrganizationType recipient
    ) {
        return switch (recipient) {
            case K12_PUBLIC_SCHOOL, K12_PRIVATE_SCHOOL -> Set.of(
                    com.northstar.funding.domain.FundingSearchCategory.PROGRAM_GRANTS,
                    com.northstar.funding.domain.FundingSearchCategory.INFRASTRUCTURE_FUNDING,
                    com.northstar.funding.domain.FundingSearchCategory.TECHNOLOGY_EQUIPMENT
            );
            case UNIVERSITY_PUBLIC, RESEARCH_INSTITUTE -> Set.of(
                    com.northstar.funding.domain.FundingSearchCategory.EDUCATION_RESEARCH,
                    com.northstar.funding.domain.FundingSearchCategory.ACADEMIC_FELLOWSHIPS,
                    com.northstar.funding.domain.FundingSearchCategory.INNOVATION_GRANTS
            );
            case PRIVATE_LANGUAGE_SCHOOL -> Set.of(
                    com.northstar.funding.domain.FundingSearchCategory.LANGUAGE_PROGRAMS,
                    com.northstar.funding.domain.FundingSearchCategory.ADULT_EDUCATION
            );
            case PRESCHOOL_EARLY_CHILDHOOD -> Set.of(
                    com.northstar.funding.domain.FundingSearchCategory.EARLY_CHILDHOOD_EDUCATION
            );
            case NGO_EDUCATION_FOCUSED, NGO_SOCIAL_SERVICES -> Set.of(
                    com.northstar.funding.domain.FundingSearchCategory.NGO_EDUCATION_PROJECTS,
                    com.northstar.funding.domain.FundingSearchCategory.COMMUNITY_PARTNERSHIPS
            );
            case LIBRARY_OR_CULTURAL_CENTER -> Set.of(
                    com.northstar.funding.domain.FundingSearchCategory.LIBRARY_RESOURCES,
                    com.northstar.funding.domain.FundingSearchCategory.ARTS_CULTURE
            );
            case INDIVIDUAL_EDUCATOR, INDIVIDUAL_STUDENT -> Set.of(
                    com.northstar.funding.domain.FundingSearchCategory.INDIVIDUAL_SCHOLARSHIPS,
                    com.northstar.funding.domain.FundingSearchCategory.ACADEMIC_FELLOWSHIPS
            );
            case FOR_PROFIT_EDUCATION, EXAMINATION_CENTER, MUNICIPALITY -> Set.of(
                    com.northstar.funding.domain.FundingSearchCategory.PROGRAM_GRANTS,
                    com.northstar.funding.domain.FundingSearchCategory.INNOVATION_GRANTS
            );
        };
    }

    private Set<com.northstar.funding.domain.FundingSearchCategory> mapMechanismToCategories(
            com.northstar.funding.domain.FundingMechanism mechanism
    ) {
        return switch (mechanism) {
            case GRANT, MATCHING_GRANT -> Set.of(
                    com.northstar.funding.domain.FundingSearchCategory.PROGRAM_GRANTS,
                    com.northstar.funding.domain.FundingSearchCategory.INNOVATION_GRANTS
            );
            case SCHOLARSHIP, FELLOWSHIP -> Set.of(
                    com.northstar.funding.domain.FundingSearchCategory.INDIVIDUAL_SCHOLARSHIPS,
                    com.northstar.funding.domain.FundingSearchCategory.ACADEMIC_FELLOWSHIPS
            );
            case LOAN -> Set.of(
                    com.northstar.funding.domain.FundingSearchCategory.STUDENT_FINANCIAL_AID
            );
            case PRIZE_AWARD -> Set.of(
                    com.northstar.funding.domain.FundingSearchCategory.INNOVATION_GRANTS
            );
            case SUBSIDY, IN_KIND_DONATION -> Set.of(
                    com.northstar.funding.domain.FundingSearchCategory.PROGRAM_GRANTS
            );
        };
    }

    /**
     * Map free-form geographic strings to GeographicScope enum.
     *
     * <p>Uses case-insensitive matching and keyword detection to infer
     * the appropriate geographic scope.
     *
     * @param geographicStrings Set of geographic strings from REST request
     * @return Most specific GeographicScope enum value
     */
    private com.northstar.funding.domain.GeographicScope mapToGeographicScope(Set<String> geographicStrings) {
        if (geographicStrings == null || geographicStrings.isEmpty()) {
            return com.northstar.funding.domain.GeographicScope.INTERNATIONAL;
        }

        // Convert all to uppercase for matching
        Set<String> upperStrings = geographicStrings.stream()
                .map(String::toUpperCase)
                .collect(java.util.stream.Collectors.toSet());

        // Check for country-specific matches (most specific)
        if (upperStrings.stream().anyMatch(s -> s.contains("BULGARIA") || s.equals("BG"))) {
            return com.northstar.funding.domain.GeographicScope.BULGARIA;
        }
        if (upperStrings.stream().anyMatch(s -> s.contains("ROMANIA") || s.equals("RO"))) {
            return com.northstar.funding.domain.GeographicScope.ROMANIA;
        }
        if (upperStrings.stream().anyMatch(s -> s.contains("GREECE") || s.equals("GR"))) {
            return com.northstar.funding.domain.GeographicScope.GREECE;
        }
        if (upperStrings.stream().anyMatch(s -> s.contains("SERBIA") || s.equals("RS"))) {
            return com.northstar.funding.domain.GeographicScope.SERBIA;
        }
        if (upperStrings.stream().anyMatch(s -> s.contains("MACEDONIA") || s.contains("NORTH MACEDONIA"))) {
            return com.northstar.funding.domain.GeographicScope.NORTH_MACEDONIA;
        }

        // Check for regional matches
        if (upperStrings.stream().anyMatch(s -> s.contains("EASTERN EUROPE") || s.contains("EAST EUROPE"))) {
            return com.northstar.funding.domain.GeographicScope.EASTERN_EUROPE;
        }
        if (upperStrings.stream().anyMatch(s -> s.contains("BALKAN") || s.contains("BALKANS"))) {
            return com.northstar.funding.domain.GeographicScope.BALKANS;
        }
        if (upperStrings.stream().anyMatch(s -> s.contains("SOUTHEASTERN EUROPE") || s.contains("SOUTHEAST EUROPE"))) {
            return com.northstar.funding.domain.GeographicScope.SOUTHEASTERN_EUROPE;
        }
        if (upperStrings.stream().anyMatch(s -> s.contains("CENTRAL EUROPE"))) {
            return com.northstar.funding.domain.GeographicScope.CENTRAL_EUROPE;
        }

        // Check for EU-related matches
        if (upperStrings.stream().anyMatch(s -> s.contains("EU MEMBER") || s.contains("EU-27"))) {
            return com.northstar.funding.domain.GeographicScope.EU_MEMBER_STATES;
        }
        if (upperStrings.stream().anyMatch(s -> s.contains("EU CANDIDATE") || s.contains("CANDIDATE COUNTRIES"))) {
            return com.northstar.funding.domain.GeographicScope.EU_CANDIDATE_COUNTRIES;
        }
        if (upperStrings.stream().anyMatch(s -> s.contains("EU ENLARGEMENT"))) {
            return com.northstar.funding.domain.GeographicScope.EU_ENLARGEMENT_REGION;
        }

        // Broader scopes
        if (upperStrings.stream().anyMatch(s -> s.contains("EUROPE") || s.equals("EU"))) {
            return com.northstar.funding.domain.GeographicScope.EUROPE;
        }
        if (upperStrings.stream().anyMatch(s -> s.contains("GLOBAL") || s.contains("WORLDWIDE"))) {
            return com.northstar.funding.domain.GeographicScope.GLOBAL;
        }

        // Default to INTERNATIONAL
        return com.northstar.funding.domain.GeographicScope.INTERNATIONAL;
    }
}
