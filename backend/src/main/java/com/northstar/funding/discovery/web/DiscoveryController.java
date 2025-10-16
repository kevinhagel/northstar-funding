package com.northstar.funding.discovery.web;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.northstar.funding.discovery.application.DiscoveryOrchestrationService;
import com.northstar.funding.discovery.domain.DiscoverySession;
import com.northstar.funding.discovery.domain.SessionStatus;
import com.northstar.funding.discovery.infrastructure.DiscoverySessionRepository;

/**
 * REST Controller: DiscoveryController
 *
 * Handles discovery session management and triggering
 * Endpoints: POST /api/discovery/trigger, GET /api/discovery/sessions
 */
@RestController
@RequestMapping("/api/discovery")
public class DiscoveryController {

    private final DiscoveryOrchestrationService orchestrationService;
    private final DiscoverySessionRepository sessionRepository;

    private static final List<String> VALID_SEARCH_ENGINES = List.of(
        "searxng", "tavily", "perplexity"
    );

    public DiscoveryController(
            DiscoveryOrchestrationService orchestrationService,
            DiscoverySessionRepository sessionRepository) {
        this.orchestrationService = orchestrationService;
        this.sessionRepository = sessionRepository;
    }

    /**
     * POST /api/discovery/trigger - Trigger manual discovery session
     */
    @PostMapping("/trigger")
    public ResponseEntity<?> triggerDiscovery(@RequestBody(required = false) DiscoveryTriggerRequest request) {

        // Default to empty lists if request is null
        List<String> searchEngines = request != null && request.searchEngines() != null
            ? request.searchEngines()
            : List.of();
        List<String> customQueries = request != null && request.customQueries() != null
            ? request.customQueries()
            : List.of();

        // Validate search engines
        for (String engine : searchEngines) {
            if (!VALID_SEARCH_ENGINES.contains(engine)) {
                return ResponseEntity.badRequest()
                        .body(new ErrorResponse("Invalid search engine: " + engine));
            }
        }

        try {
            DiscoverySession session = orchestrationService.triggerDiscovery(
                searchEngines,
                customQueries
            );

            DiscoverySessionResponse response = new DiscoverySessionResponse(
                session.getSessionId(),
                SessionStatus.RUNNING.name(),
                session.getExecutedAt(),
                45 // Estimated duration in minutes
            );

            return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse(e.getMessage()));
        }
    }

    /**
     * GET /api/discovery/sessions - List discovery sessions
     */
    @GetMapping("/sessions")
    public ResponseEntity<?> listDiscoverySessions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        // Validate pagination
        if (page < 0) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse("Invalid page number"));
        }
        if (size < 1 || size > 100) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse("Invalid page size (must be 1-100)"));
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<DiscoverySession> sessions = sessionRepository.findAll(pageable);

        return ResponseEntity.ok(sessions);
    }

    /**
     * DTOs
     */
    public record DiscoveryTriggerRequest(
        List<String> searchEngines,
        List<String> customQueries,
        List<String> targetRegions
    ) {}

    public record DiscoverySessionResponse(
        UUID sessionId,
        String status,
        LocalDateTime startedAt,
        Integer estimatedDurationMinutes
    ) {}

    public record ErrorResponse(String error) {}
}
