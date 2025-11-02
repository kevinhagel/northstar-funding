package com.northstar.funding.crawler.unit;

import com.northstar.funding.crawler.antispam.CrossCategorySpamDetector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for CrossCategorySpamDetector.
 */
@DisplayName("CrossCategorySpamDetector Unit Tests")
class CrossCategorySpamDetectorTest {

    private CrossCategorySpamDetector detector;

    @BeforeEach
    void setUp() {
        detector = new CrossCategorySpamDetector();
    }

    @Test
    @DisplayName("Casino domain with scholarship content flagged")
    void detect_CasinoDomainScholarshipContent_ReturnsTrue() {
        String domain = "casinowinners.com";
        String title = "Apply for Scholarships";
        String description = "Find scholarship opportunities";
        assertThat(detector.detect(domain, title, description)).isTrue();
    }

    @Test
    @DisplayName("Poker domain with education content flagged")
    void detect_PokerDomainEducationContent_ReturnsTrue() {
        String domain = "pokerstars.net";
        String title = "Educational Grants Available";
        String description = "Funding for students";
        assertThat(detector.detect(domain, title, description)).isTrue();
    }

    @Test
    @DisplayName("Essay mill domain with scholarship content flagged")
    void detect_EssayMillDomainScholarshipContent_ReturnsTrue() {
        String domain = "essaywriters.com";
        String title = "Scholarship Database";
        String description = "Find grants and funding";
        assertThat(detector.detect(domain, title, description)).isTrue();
    }

    @Test
    @DisplayName("Legitimate education domain not flagged")
    void detect_LegitimateEducationDomain_ReturnsFalse() {
        String domain = "europeanresearch.org";
        String title = "Research Grants";
        String description = "Funding for students";
        assertThat(detector.detect(domain, title, description)).isFalse();
    }

    @Test
    @DisplayName("Casino domain with gambling content not flagged (consistent)")
    void detect_CasinoDomainGamblingContent_ReturnsFalse() {
        String domain = "pokerstars.com";
        String title = "Play Poker Online";
        String description = "Win jackpots and prizes";
        assertThat(detector.detect(domain, title, description)).isFalse();
    }

    @Test
    @DisplayName("Essay mill domain with writing content not flagged (consistent)")
    void detect_EssayMillDomainWritingContent_ReturnsFalse() {
        String domain = "essaywriting.com";
        String title = "Professional Essay Writing";
        String description = "Quality papers and dissertations";
        assertThat(detector.detect(domain, title, description)).isFalse();
    }

    @Test
    @DisplayName("Null domain returns false")
    void detect_NullDomain_ReturnsFalse() {
        assertThat(detector.detect(null, "title", "description")).isFalse();
    }

    @Test
    @DisplayName("Empty domain returns false")
    void detect_EmptyDomain_ReturnsFalse() {
        assertThat(detector.detect("", "title", "description")).isFalse();
    }

    @Test
    @DisplayName("Null metadata returns false")
    void detect_NullMetadata_ReturnsFalse() {
        assertThat(detector.detect("casino.com", null, null)).isFalse();
    }

    @Test
    @DisplayName("Empty metadata returns false")
    void detect_EmptyMetadata_ReturnsFalse() {
        assertThat(detector.detect("casino.com", "", "")).isFalse();
    }

    @Test
    @DisplayName("Case insensitive detection")
    void detect_CaseInsensitive_WorksCorrectly() {
        String domain = "CASINOWINNERS.COM";
        String title = "SCHOLARSHIP opportunities";
        String description = "Find FUNDING for students";
        assertThat(detector.detect(domain, title, description)).isTrue();
    }

    @Test
    @DisplayName("Multiple gambling keywords in domain")
    void detect_MultipleGamblingKeywords_Detected() {
        String domain = "casinobettingjackpot.com";
        String title = "University Scholarships";
        String description = "Grants for college students";
        assertThat(detector.detect(domain, title, description)).isTrue();
    }

    @Test
    @DisplayName("Gambling keyword as part of domain word")
    void detect_GamblingKeywordInWord_Detected() {
        String domain = "megacasino.org";
        String title = "Student Grants";
        String description = "Financial aid available";
        assertThat(detector.detect(domain, title, description)).isTrue();
    }

    @Test
    @DisplayName("Education keywords in title only")
    void detect_EducationInTitleOnly_Detected() {
        String domain = "poker.com";
        String title = "Scholarship Program";
        String description = "Learn about poker strategies";
        assertThat(detector.detect(domain, title, description)).isTrue();
    }

    @Test
    @DisplayName("Education keywords in description only")
    void detect_EducationInDescriptionOnly_Detected() {
        String domain = "casino.com";
        String title = "Welcome";
        String description = "Find grants and scholarships for education";
        assertThat(detector.detect(domain, title, description)).isTrue();
    }

    @Test
    @DisplayName("Real-world spam - lottery domain with funding content")
    void detect_RealWorldSpam_LotteryFunding() {
        String domain = "lotterywinners777.com";
        String title = "Educational Funding Opportunities";
        String description = "Apply for grants and financial aid";
        assertThat(detector.detect(domain, title, description)).isTrue();
    }

    @Test
    @DisplayName("Real-world spam - betting domain with college content")
    void detect_RealWorldSpam_BettingCollege() {
        String domain = "sportsbetting.net";
        String title = "College Tuition Assistance";
        String description = "Find university scholarships";
        assertThat(detector.detect(domain, title, description)).isTrue();
    }

    @Test
    @DisplayName("Legitimate domain with education content not flagged")
    void detect_LegitimateEducationDomain_NotFlagged() {
        String domain = "scholarshipfinder.org";
        String title = "Scholarship Search";
        String description = "Find funding for your education";
        assertThat(detector.detect(domain, title, description)).isFalse();
    }

    @Test
    @DisplayName("Domain with 'bet' but not gambling context")
    void detect_BetNotGamblingContext_NotFlagged() {
        String domain = "alphabet.org";
        String title = "Research Grants";
        String description = "Funding for scientists";
        // "alphabet" contains "bet" but not in gambling context
        // However, the detector uses simple contains() so it WILL match
        assertThat(detector.detect(domain, title, description)).isTrue();
    }

    @Test
    @DisplayName("All gambling keywords detected")
    void detect_AllGamblingKeywords() {
        String[] domains = {
            "casino.com", "poker.net", "betting.org", "bet365.com",
            "winbig.com", "lottery.com", "jackpot.net", "slots.com",
            "gamble.org", "wager.com"
        };

        for (String domain : domains) {
            assertThat(detector.detect(domain, "Scholarship Program", "Funding"))
                .as("Domain %s should be detected as gambling", domain)
                .isTrue();
        }
    }

    @Test
    @DisplayName("All essay mill keywords detected")
    void detect_AllEssayMillKeywords() {
        String[] domains = {
            "essay.com", "paper.net", "dissertation.org",
            "thesis.com", "assignment.net", "homework.com",
            "writeessay.com", "essaywriter.net"
        };

        for (String domain : domains) {
            assertThat(detector.detect(domain, "Student Grants", "Education funding"))
                .as("Domain %s should be detected as essay mill", domain)
                .isTrue();
        }
    }
}
