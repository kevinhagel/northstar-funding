package com.northstar.funding.rest.dto;

import java.util.UUID;

/**
 * Response model for search execution endpoint.
 *
 * <p>Returns tracking information for the initiated search workflow:
 * <ul>
 *   <li>sessionId: UUID for tracking this discovery session</li>
 *   <li>queriesGenerated: Number of search queries created</li>
 *   <li>status: Workflow status (INITIATED)</li>
 *   <li>message: Human-readable status message</li>
 * </ul>
 */
public record SearchExecutionResponse(
        UUID sessionId,
        int queriesGenerated,
        String status,
        String message
) {
    public static SearchExecutionResponse initiated(UUID sessionId, int queriesGenerated) {
        return new SearchExecutionResponse(
                sessionId,
                queriesGenerated,
                "INITIATED",
                String.format("Search workflow initiated with %d queries. Use sessionId to track progress.", queriesGenerated)
        );
    }
}
