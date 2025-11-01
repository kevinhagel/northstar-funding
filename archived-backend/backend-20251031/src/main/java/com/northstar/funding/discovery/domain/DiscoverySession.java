package com.northstar.funding.discovery.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.util.List;
import java.util.Set;

import java.util.Map;

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
 * - Search engine integration tracking (Searxng, Tavily, Perplexity)
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
    private LocalDateTime executedAt;
    
    @Column("executed_by")
    private String executedBy; // System/scheduler identifier
    
    @Column("session_type")
    private SessionType sessionType;
    
    // Execution Status & Timing
    private SessionStatus status;
    
    @Column("duration_minutes")
    private Integer durationMinutes;
    
    @Column("started_at")
    private LocalDateTime startedAt;
    
    @Column("completed_at")
    private LocalDateTime completedAt;
    
    // Search Configuration (AI Query Generation Context)
    @Column("search_engines_used")
    private Set<String> searchEnginesUsed; // Set of search engines: {"searxng", "tavily", "perplexity"}
    
    @Column("search_queries")
    private List<String> searchQueries; // List of generated queries executed
    
    @Column("query_generation_prompt")
    private String queryGenerationPrompt; // AI prompt used for query generation
    
    // Results & Performance Metrics
    @Column("candidates_found")
    private Integer candidatesFound;
    
    @Column("duplicates_detected")
    private Integer duplicatesDetected;
    
    @Column("sources_scraped")
    private Integer sourcesScraped;
    
    /**
     * Average AI confidence score of candidates found (0.00-1.00)
     * Uses BigDecimal with scale 2 for precise decimal arithmetic
     */
    @Column("average_confidence_score")
    private BigDecimal averageConfidenceScore;
    
    // Error Handling & Diagnostics
    @Column("error_messages")
    private List<String> errorMessages; // List of error messages
    
    @Column("search_engine_failures")
    private String searchEngineFailures; // JSON string: {"engine": ["error1", "error2"]}
    
    // Process Metadata (for AI improvement)
    @Column("llm_model_used")
    private String llmModelUsed; // LM Studio model used for query generation
    
    @Column("search_parameters")
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
