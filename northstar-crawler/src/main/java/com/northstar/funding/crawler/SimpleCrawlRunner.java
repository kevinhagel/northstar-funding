package com.northstar.funding.crawler;

import com.northstar.funding.crawler.service.ScheduledCrawlService;
import com.northstar.funding.crawler.service.ScheduledCrawlService.CrawlResult;
import com.northstar.funding.domain.SessionType;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Profile;

/**
 * Simple command-line runner for testing scheduled crawl functionality.
 *
 * Usage:
 *   mvn spring-boot:run -pl northstar-crawler
 *   mvn spring-boot:run -pl northstar-crawler -Dspring-boot.run.arguments="custom search query"
 *
 * Or with run-crawl.sh wrapper:
 *   ./run-crawl.sh
 *   ./run-crawl.sh "EU funding opportunities"
 *
 * This runner executes the complete scheduled crawl flow:
 * 1. Multi-provider search execution
 * 2. Result processing with confidence scoring
 * 3. Candidate creation (high confidence >= 0.60)
 * 4. Domain registration and blacklist management
 * 5. Statistics tracking
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
    @Profile("!test")
    public CommandLineRunner executeCrawl(ScheduledCrawlService crawlService) {
        return args -> {
            System.out.println("\n========================================");
            System.out.println("NorthStar Funding Discovery - Crawl Runner");
            System.out.println("========================================\n");

            // Parse query from arguments
            String query = args.length > 0
                ? String.join(" ", args)
                : "EU funding opportunities Bulgaria 2025";

            System.out.println("Query: " + query);
            System.out.println("Max results per provider: 20");
            System.out.println("Providers: BraveSearch, SearXNG, Serper, Perplexica, Perplexica");
            System.out.println();

            System.out.println("\n--- Executing Scheduled Crawl ---\n");

            // Execute complete crawl
            CrawlResult result = crawlService.executeCrawl(
                    query,
                    SessionType.MANUAL,
                    "SimpleCrawlRunner"
            );

            if (result.success()) {
                System.out.println("\n--- Crawl Results ---");
                System.out.println("Session ID: " + result.sessionId());
                System.out.println("Duration: " + result.durationMs() + "ms");
                System.out.println();
                System.out.println("High-confidence candidates created: " + result.highConfidenceCandidatesCreated());
                System.out.println("Low-confidence candidates created: " + result.lowConfidenceCandidatesCreated());
                System.out.println("Total candidates: " + (result.highConfidenceCandidatesCreated() + result.lowConfidenceCandidatesCreated()));
                System.out.println();
                System.out.println("Spam TLD filtered: " + result.spamFiltered());
                System.out.println("Duplicates skipped: " + result.duplicatesSkipped());
                System.out.println("Blacklisted skipped: " + result.blacklistedSkipped());

                System.out.println("\n✅ Crawl completed successfully!");
                System.out.println("========================================\n");

            } else {
                System.err.println("\n❌ Crawl failed:");
                System.err.println("Session ID: " + result.sessionId());
                System.err.println("Error: " + result.errorMessage());

                System.out.println("========================================\n");
                System.exit(1);
            }
        };
    }
}
