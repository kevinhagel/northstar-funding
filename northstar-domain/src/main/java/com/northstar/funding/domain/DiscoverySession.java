package com.northstar.funding.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.List;
import java.util.Set;
import java.util.Map;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Discovery Session Domain Entity
 * 
 * Audit record of automated discovery executions for tracking and improvement.
 * Critical for AI process improvement and error analysis.
 * 
 * Key Features:
 * - Search engine integration tracking (Searxng, Perplexica, Perplexity)
 * - LM Studio integration metadata (model tracking, prompt engineering)
 * - Performance metrics for process optimization
 * - Spring Data JDBC entity (no ORM complexity)
 */

@Table("discovery_session")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiscoverySession {

    // Primary Identity
    @Id
    @Column("session_id")
    private UUID sessionId;

    @Column("executed_at")
    @Builder.Default
    private LocalDateTime executedAt = LocalDateTime.now();

    @Column("executed_by")
    @Builder.Default
    private String executedBy = "SYSTEM";

    @Column("session_type")
    @Builder.Default
    private SessionType sessionType = SessionType.SCHEDULED;

    // Execution Status & Timing
    @Builder.Default
    private SessionStatus status = SessionStatus.RUNNING;

    @Column("duration_minutes")
    private Integer durationMinutes;

    @Column("started_at")
    @Builder.Default
    private LocalDateTime startedAt = LocalDateTime.now();

    @Column("completed_at")
    private LocalDateTime completedAt;

    // Search Configuration (AI Query Generation Context)
    @Column("search_engines_used")
    @Builder.Default
    private Set<String> searchEnginesUsed = Set.of();

    @Column("search_queries")
    @Builder.Default
    private List<String> searchQueries = List.of();

    @Column("query_generation_prompt")
    private String queryGenerationPrompt;

    // Results & Performance Metrics
    @Column("candidates_found")
    @Builder.Default
    private Integer candidatesFound = 0;

    @Column("duplicates_detected")
    @Builder.Default
    private Integer duplicatesDetected = 0;
    @Column("sources_scraped")
    @Builder.Default
    private Integer sourcesScraped = 0;
    
    /**
     * Average AI confidence score of candidates found (0.00-1.00)
     * Uses BigDecimal with scale 2 for precise decimal arithmetic
     */
    
    private BigDecimal averageConfidenceScore;
    
    // Error Handling & Diagnostics
    
    private List<String> errorMessages; // List of error messages
    
    
    private String searchEngineFailures; // JSON string: {"engine": ["error1", "error2"]}
    
    // Process Metadata (for AI improvement)
    
    private String llmModelUsed; // LM Studio model used for query generation
    
    
    private String searchParameters; // JSON string: {"param": "value"}

    // Business methods
    public boolean isCompleted() {
        return status == SessionStatus.COMPLETED;
    }
    
    public boolean hasFailed() {
        return status == SessionStatus.FAILED;
    }
    
    public boolean isRunning() {
        return status == SessionStatus.RUNNING;
    }
    
    public void markCompleted() {
        this.status = SessionStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
        if (this.startedAt != null) {
            long minutes = java.time.Duration.between(this.startedAt, this.completedAt).toMinutes();
            this.durationMinutes = (int) minutes;
        }
    }
    
    public void markFailed() {
        this.status = SessionStatus.FAILED;
        this.completedAt = LocalDateTime.now();
        if (this.startedAt != null) {
            long minutes = java.time.Duration.between(this.startedAt, this.completedAt).toMinutes();
            this.durationMinutes = (int) minutes;
        }
    }
}
