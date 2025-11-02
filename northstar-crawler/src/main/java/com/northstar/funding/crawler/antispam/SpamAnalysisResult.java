package com.northstar.funding.crawler.antispam;

/**
 * Result of spam analysis with verdict and detection reasons.
 */
public record SpamAnalysisResult(
    boolean isSpam,                    // true if spam detected, false otherwise
    String rejectionReason,            // Human-readable reason (e.g., "Keyword stuffing detected")
    SpamIndicator primaryIndicator,    // Primary spam indicator that triggered rejection
    double confidenceScore             // Confidence in spam detection (0.0-1.0)
) {
    /**
     * Create SPAM result with reason.
     */
    public static SpamAnalysisResult spam(SpamIndicator indicator, String reason, double confidence) {
        return new SpamAnalysisResult(true, reason, indicator, confidence);
    }

    /**
     * Create NOT-SPAM result.
     */
    public static SpamAnalysisResult notSpam() {
        return new SpamAnalysisResult(false, null, null, 0.0);
    }
}
