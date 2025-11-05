package com.northstar.funding.crawler.scoring;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for DomainCredibilityService TLD scoring
 * Based on 5-tier TLD classification from research (specs/006-search-result-processing/tld-credibility-research.md)
 */
class DomainCredibilityServiceTest {

    private DomainCredibilityService service;

    @BeforeEach
    void setUp() {
        service = new DomainCredibilityService();
    }

    @Nested
    @DisplayName("Tier 1: Highest Credibility (+0.20)")
    class Tier1Tests {

        @Test
        @DisplayName("Validated nonprofit TLDs: .ngo")
        void testNgoTld() {
            BigDecimal score = service.getTldScore("https://example.ngo");
            assertThat(score).isEqualByComparingTo(new BigDecimal("0.20"));
        }

        @Test
        @DisplayName("Validated nonprofit TLDs: .foundation")
        void testFoundationTld() {
            BigDecimal score = service.getTldScore("https://example.foundation");
            assertThat(score).isEqualByComparingTo(new BigDecimal("0.20"));
        }

        @Test
        @DisplayName("Validated nonprofit TLDs: .charity")
        void testCharityTld() {
            BigDecimal score = service.getTldScore("https://example.charity");
            assertThat(score).isEqualByComparingTo(new BigDecimal("0.20"));
        }

        @Test
        @DisplayName("Government domain: .gov")
        void testGovTld() {
            BigDecimal score = service.getTldScore("https://example.gov");
            assertThat(score).isEqualByComparingTo(new BigDecimal("0.20"));
        }

        @Test
        @DisplayName("Government second-level domain: .gov.bg")
        void testGovBgSecondLevel() {
            BigDecimal score = service.getTldScore("https://ministry.gov.bg");
            assertThat(score).isEqualByComparingTo(new BigDecimal("0.20"));
        }

        @Test
        @DisplayName("EU institution domain: .europa.eu")
        void testEuropaEu() {
            BigDecimal score = service.getTldScore("https://european-union.europa.eu");
            assertThat(score).isEqualByComparingTo(new BigDecimal("0.20"));
        }

        @Test
        @DisplayName("Education domain: .edu")
        void testEduTld() {
            BigDecimal score = service.getTldScore("https://example.edu");
            assertThat(score).isEqualByComparingTo(new BigDecimal("0.20"));
        }

        @Test
        @DisplayName("Education second-level domain: .edu.bg")
        void testEduBgSecondLevel() {
            BigDecimal score = service.getTldScore("https://university.edu.bg");
            assertThat(score).isEqualByComparingTo(new BigDecimal("0.20"));
        }
    }

    @Nested
    @DisplayName("Tier 2: High Credibility (+0.15)")
    class Tier2Tests {

        @Test
        @DisplayName("Traditional nonprofit: .org")
        void testOrgTld() {
            BigDecimal score = service.getTldScore("https://example.org");
            assertThat(score).isEqualByComparingTo(new BigDecimal("0.15"));
        }

        @Test
        @DisplayName("EU domain: .eu")
        void testEuTld() {
            BigDecimal score = service.getTldScore("https://example.eu");
            assertThat(score).isEqualByComparingTo(new BigDecimal("0.15"));
        }

        @Test
        @DisplayName("Bulgaria ccTLD: .bg")
        void testBgTld() {
            BigDecimal score = service.getTldScore("https://fulbright.bg");
            assertThat(score).isEqualByComparingTo(new BigDecimal("0.15"));
        }

        @Test
        @DisplayName("Romania ccTLD: .ro")
        void testRoTld() {
            BigDecimal score = service.getTldScore("https://example.ro");
            assertThat(score).isEqualByComparingTo(new BigDecimal("0.15"));
        }

        @Test
        @DisplayName("Poland ccTLD: .pl")
        void testPlTld() {
            BigDecimal score = service.getTldScore("https://example.pl");
            assertThat(score).isEqualByComparingTo(new BigDecimal("0.15"));
        }

        @Test
        @DisplayName("Czech Republic ccTLD: .cz")
        void testCzTld() {
            BigDecimal score = service.getTldScore("https://eeagrants.cz");
            assertThat(score).isEqualByComparingTo(new BigDecimal("0.15"));
        }

        @Test
        @DisplayName("Funding-specific TLD: .fund")
        void testFundTld() {
            BigDecimal score = service.getTldScore("https://example.fund");
            assertThat(score).isEqualByComparingTo(new BigDecimal("0.15"));
        }

        @Test
        @DisplayName("Bulgaria Cyrillic IDN: .бг")
        void testBgCyrillicTld() {
            BigDecimal score = service.getTldScore("https://example.бг");
            assertThat(score).isEqualByComparingTo(new BigDecimal("0.15"));
        }
    }

    @Nested
    @DisplayName("Tier 3: Medium Credibility (+0.08)")
    class Tier3Tests {

        @Test
        @DisplayName("Generic business: .com")
        void testComTld() {
            BigDecimal score = service.getTldScore("https://example.com");
            assertThat(score).isEqualByComparingTo(new BigDecimal("0.08"));
        }

        @Test
        @DisplayName("Generic business: .net")
        void testNetTld() {
            BigDecimal score = service.getTldScore("https://example.net");
            assertThat(score).isEqualByComparingTo(new BigDecimal("0.08"));
        }

        @Test
        @DisplayName("Information site: .info")
        void testInfoTld() {
            BigDecimal score = service.getTldScore("https://example.info");
            assertThat(score).isEqualByComparingTo(new BigDecimal("0.08"));
        }
    }

    @Nested
    @DisplayName("Tier 4: Low Credibility (0.00)")
    class Tier4Tests {

        @Test
        @DisplayName("Cheap business: .biz")
        void testBizTld() {
            BigDecimal score = service.getTldScore("https://example.biz");
            assertThat(score).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("Commercial: .co")
        void testCoTld() {
            BigDecimal score = service.getTldScore("https://example.co");
            assertThat(score).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("Tech startups: .io")
        void testIoTld() {
            BigDecimal score = service.getTldScore("https://example.io");
            assertThat(score).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }

    @Nested
    @DisplayName("Tier 5: Negative Credibility (Spam TLDs)")
    class Tier5Tests {

        @Test
        @DisplayName("Freenom free domain: .tk")
        void testTkTld() {
            BigDecimal score = service.getTldScore("https://example.tk");
            assertThat(score).isEqualByComparingTo(new BigDecimal("-0.30"));
        }

        @Test
        @DisplayName("Freenom free domain: .ml")
        void testMlTld() {
            BigDecimal score = service.getTldScore("https://example.ml");
            assertThat(score).isEqualByComparingTo(new BigDecimal("-0.30"));
        }

        @Test
        @DisplayName("Freenom free domain: .ga")
        void testGaTld() {
            BigDecimal score = service.getTldScore("https://example.ga");
            assertThat(score).isEqualByComparingTo(new BigDecimal("-0.30"));
        }

        @Test
        @DisplayName("Cheap phishing favorite: .xyz")
        void testXyzTld() {
            BigDecimal score = service.getTldScore("https://example.xyz");
            assertThat(score).isEqualByComparingTo(new BigDecimal("-0.20"));
        }

        @Test
        @DisplayName("Cheap phishing favorite: .top")
        void testTopTld() {
            BigDecimal score = service.getTldScore("https://example.top");
            assertThat(score).isEqualByComparingTo(new BigDecimal("-0.20"));
        }

        @Test
        @DisplayName("Loan scam favorite: .loan")
        void testLoanTld() {
            BigDecimal score = service.getTldScore("https://example.loan");
            assertThat(score).isEqualByComparingTo(new BigDecimal("-0.25"));
        }

        @Test
        @DisplayName("Suspicious click domain: .click")
        void testClickTld() {
            BigDecimal score = service.getTldScore("https://example.click");
            assertThat(score).isEqualByComparingTo(new BigDecimal("-0.15"));
        }
    }

    @Nested
    @DisplayName("Edge Cases and URL Parsing")
    class EdgeCaseTests {

        @Test
        @DisplayName("Handle subdomain correctly")
        void testSubdomain() {
            BigDecimal score = service.getTldScore("https://scholarships.su.bg/apply");
            assertThat(score).isEqualByComparingTo(new BigDecimal("0.15")); // .bg is Tier 2
        }

        @Test
        @DisplayName("Handle port numbers")
        void testPortNumber() {
            BigDecimal score = service.getTldScore("https://example.org:8080");
            assertThat(score).isEqualByComparingTo(new BigDecimal("0.15")); // .org is Tier 2
        }

        @Test
        @DisplayName("Handle query parameters")
        void testQueryParameters() {
            BigDecimal score = service.getTldScore("https://example.ngo?page=funding");
            assertThat(score).isEqualByComparingTo(new BigDecimal("0.20")); // .ngo is Tier 1
        }

        @Test
        @DisplayName("Handle paths")
        void testPaths() {
            BigDecimal score = service.getTldScore("https://example.foundation/grants/apply");
            assertThat(score).isEqualByComparingTo(new BigDecimal("0.20")); // .foundation is Tier 1
        }

        @Test
        @DisplayName("Case insensitivity: .ORG")
        void testCaseInsensitivityUppercase() {
            BigDecimal score = service.getTldScore("https://EXAMPLE.ORG");
            assertThat(score).isEqualByComparingTo(new BigDecimal("0.15"));
        }

        @Test
        @DisplayName("Case insensitivity: .Bg")
        void testCaseInsensitivityMixedCase() {
            BigDecimal score = service.getTldScore("https://example.Bg");
            assertThat(score).isEqualByComparingTo(new BigDecimal("0.15"));
        }

        @Test
        @DisplayName("Unknown TLD defaults to 0.00")
        void testUnknownTld() {
            BigDecimal score = service.getTldScore("https://example.unknowntld");
            assertThat(score).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("Invalid URL returns 0.00")
        void testInvalidUrl() {
            BigDecimal score = service.getTldScore("not-a-url");
            assertThat(score).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("Null URL returns 0.00")
        void testNullUrl() {
            BigDecimal score = service.getTldScore(null);
            assertThat(score).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("Empty URL returns 0.00")
        void testEmptyUrl() {
            BigDecimal score = service.getTldScore("");
            assertThat(score).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }

    @Nested
    @DisplayName("BigDecimal Precision")
    class PrecisionTests {

        @Test
        @DisplayName("All scores have scale 2")
        void testScale2Precision() {
            assertThat(service.getTldScore("https://example.org").scale()).isEqualTo(2);
            assertThat(service.getTldScore("https://example.ngo").scale()).isEqualTo(2);
            assertThat(service.getTldScore("https://example.bg").scale()).isEqualTo(2);
            assertThat(service.getTldScore("https://example.xyz").scale()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("Helper Methods")
    class HelperMethodTests {

        @Test
        @DisplayName("isSpamTld detects Tier 5 TLDs")
        void testIsSpamTld() {
            assertThat(service.isSpamTld("https://example.xyz")).isTrue();
            assertThat(service.isSpamTld("https://example.tk")).isTrue();
            assertThat(service.isSpamTld("https://example.loan")).isTrue();

            assertThat(service.isSpamTld("https://example.org")).isFalse();
            assertThat(service.isSpamTld("https://example.bg")).isFalse();
        }

        @Test
        @DisplayName("isValidatedNonprofit detects .ngo/.foundation/.charity")
        void testIsValidatedNonprofit() {
            assertThat(service.isValidatedNonprofit("https://example.ngo")).isTrue();
            assertThat(service.isValidatedNonprofit("https://example.foundation")).isTrue();
            assertThat(service.isValidatedNonprofit("https://example.charity")).isTrue();

            assertThat(service.isValidatedNonprofit("https://example.org")).isFalse();
            assertThat(service.isValidatedNonprofit("https://example.com")).isFalse();
        }

        @Test
        @DisplayName("isGovernmentDomain detects .gov and .gov.xx")
        void testIsGovernmentDomain() {
            assertThat(service.isGovernmentDomain("https://example.gov")).isTrue();
            assertThat(service.isGovernmentDomain("https://ministry.gov.bg")).isTrue();
            assertThat(service.isGovernmentDomain("https://agency.gov.ro")).isTrue();
            assertThat(service.isGovernmentDomain("https://european-union.europa.eu")).isTrue();

            assertThat(service.isGovernmentDomain("https://example.org")).isFalse();
            assertThat(service.isGovernmentDomain("https://example.bg")).isFalse();
        }

        @Test
        @DisplayName("isTargetRegionCcTld detects Eastern Europe ccTLDs")
        void testIsTargetRegionCcTld() {
            assertThat(service.isTargetRegionCcTld("https://example.bg")).isTrue();
            assertThat(service.isTargetRegionCcTld("https://example.ro")).isTrue();
            assertThat(service.isTargetRegionCcTld("https://example.pl")).isTrue();
            assertThat(service.isTargetRegionCcTld("https://example.cz")).isTrue();
            assertThat(service.isTargetRegionCcTld("https://example.eu")).isTrue();

            assertThat(service.isTargetRegionCcTld("https://example.com")).isFalse();
            assertThat(service.isTargetRegionCcTld("https://example.us")).isFalse();
        }
    }
}
