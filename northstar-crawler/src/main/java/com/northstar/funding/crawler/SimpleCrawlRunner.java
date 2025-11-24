package com.northstar.funding.crawler;

import com.northstar.funding.crawler.orchestrator.MultiProviderSearchOrchestrator;
import com.northstar.funding.crawler.orchestrator.SearchExecutionResult;
import com.northstar.funding.domain.DiscoverySession;
import com.northstar.funding.domain.FundingSourceCandidate;
import com.northstar.funding.domain.SearchResult;
import com.northstar.funding.domain.SessionStatus;
import com.northstar.funding.domain.SessionType;
import com.northstar.funding.persistence.repository.DiscoverySessionRepository;
import com.northstar.funding.persistence.repository.FundingSourceCandidateRepository;
import com.northstar.funding.persistence.repository.SearchResultRepository;
import io.vavr.control.Try;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Simple command-line application to execute a search and populate the database.
 *
 * This is a minimal runner for testing and manual execution.
 * For production scheduling, see NightlyDiscoveryScheduler.
 */
@SpringBootApplication
@ComponentScan(basePackages = {
    "com.northstar.funding.crawler",
    "com.northstar.funding.persistence"
})
public class SimpleCrawlRunner {

    public static void main(String[] args) {
        SpringApplication.run(SimpleCrawlRunner.class, args);
    }

    @Bean
    public CommandLineRunner executeSearch(
        MultiProviderSearchOrchestrator orchestrator,
        DiscoverySessionRepository sessionRepository,
        SearchResultRepository searchResultRepository,
        FundingSourceCandidateRepository candidateRepository
    ) {
        return args -> {
            System.out.println("\n========================================");
            System.out.println("SIMPLE CRAWL RUNNER");
            System.out.println("========================================\n");

            // Get query from command line args or use default
            String query = args.length > 0
                ? String.join(" ", args)
                : "EU funding opportunities Bulgaria 2025";

            System.out.println("Query: " + query);
            System.out.println("----------------------------------------\n");

            // Create discovery session
            DiscoverySession session = DiscoverySession.builder()
                .sessionType(SessionType.MANUAL)
                .executedBy("SimpleCrawlRunner")
                .startedAt(LocalDateTime.now())
                .status(SessionStatus.RUNNING)
                .build();

            DiscoverySession savedSession = sessionRepository.save(session);
            UUID sessionId = savedSession.getSessionId();
            System.out.println("Created session: " + sessionId);

            // Execute search
            Try<SearchExecutionResult> result = orchestrator.executeMultiProviderSearch(
                query,    // keyword query
                null,     // ai-optimized query (optional)
                20,       // max results per provider
                sessionId
            );

            if (result.isSuccess()) {
                SearchExecutionResult executionResult = result.get();

                System.out.println("\nSearch completed successfully:");
                System.out.println("  - Provider failures: " + executionResult.providerErrors().size());
                System.out.println("  - Total results: " + executionResult.successfulResults().size());

                // Save search results
                List<SearchResult> searchResults = executionResult.successfulResults();
                if (!searchResults.isEmpty()) {
                    searchResultRepository.saveAll(searchResults);
                    System.out.println("  - Saved " + searchResults.size() + " search results to database");
                }

                // Count candidates created (orchestrator should have created them)
                List<FundingSourceCandidate> candidates = candidateRepository.findByDiscoverySessionId(sessionId);
                System.out.println("  - Candidates created: " + candidates.size());

                // Update session status
                DiscoverySession updatedSession = sessionRepository.findById(sessionId).orElseThrow();
                updatedSession.markCompleted();
                updatedSession.setCandidatesFound(candidates.size());
                sessionRepository.save(updatedSession);

                System.out.println("\nSession completed: " + sessionId);
                System.out.println("========================================\n");
            } else {
                System.err.println("\nSearch failed:");
                System.err.println("  - Error: " + result.getCause().getMessage());

                // Update session as failed
                DiscoverySession updatedSession = sessionRepository.findById(sessionId).orElseThrow();
                updatedSession.markFailed();
                sessionRepository.save(updatedSession);

                System.out.println("\nSession failed: " + sessionId);
                System.out.println("========================================\n");
            }
        };
    }
}
