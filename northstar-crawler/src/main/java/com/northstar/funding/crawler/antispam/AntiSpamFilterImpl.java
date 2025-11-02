package com.northstar.funding.crawler.antispam;

import com.northstar.funding.domain.SearchResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Implementation of AntiSpamFilter that orchestrates 4 spam detection strategies.
 *
 * Detection Strategies:
 * 1. Keyword Stuffing - Excessive keyword repetition (unique ratio < 0.5)
 * 2. Domain-Metadata Mismatch - Unrelated domain and metadata (cosine similarity < 0.15)
 * 3. Unnatural Keyword List - Missing natural language structure (< 2 common words)
 * 4. Cross-Category Spam - Scammer industry domain + education metadata
 *
 * Returns isSpam=true if ANY strategy detects spam.
 * Primary indicator is the FIRST detection that triggered.
 */
@Service
@Slf4j
public class AntiSpamFilterImpl implements AntiSpamFilter {

    private final KeywordStuffingDetector keywordStuffingDetector;
    private final DomainMetadataMismatchDetector domainMetadataMismatchDetector;
    private final UnnaturalKeywordListDetector unnaturalKeywordListDetector;
    private final CrossCategorySpamDetector crossCategorySpamDetector;

    public AntiSpamFilterImpl(
            KeywordStuffingDetector keywordStuffingDetector,
            DomainMetadataMismatchDetector domainMetadataMismatchDetector,
            UnnaturalKeywordListDetector unnaturalKeywordListDetector,
            CrossCategorySpamDetector crossCategorySpamDetector
    ) {
        this.keywordStuffingDetector = keywordStuffingDetector;
        this.domainMetadataMismatchDetector = domainMetadataMismatchDetector;
        this.unnaturalKeywordListDetector = unnaturalKeywordListDetector;
        this.crossCategorySpamDetector = crossCategorySpamDetector;
    }

    @Override
    public SpamAnalysisResult analyzeForSpam(SearchResult result) {
        if (result == null) {
            return SpamAnalysisResult.notSpam();
        }

        long startTime = System.nanoTime();
        int detectionCount = 0;
        SpamIndicator primaryIndicator = null;
        String rejectionReason = null;

        // Combine title + description for text analysis
        String combinedText = (result.getTitle() != null ? result.getTitle() : "") + " " +
                              (result.getDescription() != null ? result.getDescription() : "");

        // Strategy 1: Keyword Stuffing
        if (detectKeywordStuffing(combinedText)) {
            detectionCount++;
            if (primaryIndicator == null) {
                primaryIndicator = SpamIndicator.KEYWORD_STUFFING;
                rejectionReason = "Keyword stuffing detected: excessive keyword repetition (unique ratio < 0.5)";
            }
        }

        // Strategy 2: Domain-Metadata Mismatch
        if (detectDomainMetadataMismatch(result.getDomain(), result.getTitle(), result.getDescription())) {
            detectionCount++;
            if (primaryIndicator == null) {
                primaryIndicator = SpamIndicator.DOMAIN_METADATA_MISMATCH;
                rejectionReason = "Domain-metadata mismatch: domain keywords unrelated to page content (similarity < 0.15)";
            }
        }

        // Strategy 3: Unnatural Keyword List
        if (detectUnnaturalKeywordList(combinedText)) {
            detectionCount++;
            if (primaryIndicator == null) {
                primaryIndicator = SpamIndicator.UNNATURAL_KEYWORD_LIST;
                rejectionReason = "Unnatural keyword list: missing natural language structure (< 2 common words)";
            }
        }

        // Strategy 4: Cross-Category Spam
        if (detectCrossCategorySpam(result.getDomain(), result.getTitle(), result.getDescription())) {
            detectionCount++;
            if (primaryIndicator == null) {
                primaryIndicator = SpamIndicator.CROSS_CATEGORY_SPAM;
                rejectionReason = "Cross-category spam: scammer industry domain with education content";
            }
        }

        long durationNanos = System.nanoTime() - startTime;
        long durationMs = durationNanos / 1_000_000;

        boolean isSpam = detectionCount > 0;

        if (isSpam) {
            log.debug("Spam detected: domain={}, indicator={}, duration={}ms",
                    result.getDomain(), primaryIndicator, durationMs);
        }

        // Calculate confidence score based on number of detections
        double confidence = isSpam ? Math.min(1.0, detectionCount * 0.35) : 0.0;

        return isSpam ?
                SpamAnalysisResult.spam(primaryIndicator, rejectionReason, confidence) :
                SpamAnalysisResult.notSpam();
    }

    @Override
    public boolean detectKeywordStuffing(String text) {
        return keywordStuffingDetector.detect(text);
    }

    @Override
    public boolean detectDomainMetadataMismatch(String domain, String title, String description) {
        return domainMetadataMismatchDetector.detect(domain, title, description);
    }

    @Override
    public boolean detectUnnaturalKeywordList(String text) {
        return unnaturalKeywordListDetector.detect(text);
    }

    @Override
    public boolean detectCrossCategorySpam(String domain, String title, String description) {
        return crossCategorySpamDetector.detect(domain, title, description);
    }
}
