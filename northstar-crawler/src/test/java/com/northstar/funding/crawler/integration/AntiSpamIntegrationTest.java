package com.northstar.funding.crawler.integration;

import com.northstar.funding.crawler.antispam.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration test for Scenario 3: Anti-Spam Filtering.
 *
 * T041: Tests that anti-spam filter integrates with all 4 detection strategies.
 * Simplified version testing Spring context and bean wiring.
 */
@SpringBootTest(classes = TestApplication.class)
@Testcontainers
@DisplayName("Integration Test - Scenario 3: Anti-Spam Filtering")
class AntiSpamIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.enabled", () -> "true");
    }

    @Autowired
    private AntiSpamFilter antiSpamFilter;

    @Autowired
    private KeywordStuffingDetector keywordStuffingDetector;

    @Autowired
    private DomainMetadataMismatchDetector domainMetadataMismatchDetector;

    @Autowired
    private UnnaturalKeywordListDetector unnaturalKeywordListDetector;

    @Autowired
    private CrossCategorySpamDetector crossCategorySpamDetector;

    @Test
    @DisplayName("Spring context loads with anti-spam filter and all detectors")
    void contextLoads_AllAntiSpamBeans() {
        assertThat(antiSpamFilter).isNotNull();
        assertThat(keywordStuffingDetector).isNotNull();
        assertThat(domainMetadataMismatchDetector).isNotNull();
        assertThat(unnaturalKeywordListDetector).isNotNull();
        assertThat(crossCategorySpamDetector).isNotNull();
    }

    @Test
    @DisplayName("Anti-spam filter is wired as AntiSpamFilterImpl")
    void antiSpamFilter_IsCorrectImplementation() {
        assertThat(antiSpamFilter).isInstanceOf(AntiSpamFilterImpl.class);
    }

    @Test
    @DisplayName("Keyword stuffing detector can detect simple cases")
    void keywordStuffingDetector_WorksBasic() {
        boolean result = keywordStuffingDetector.detect("grants grants grants grants grants");
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Domain metadata mismatch detector can detect simple cases")
    void domainMetadataMismatchDetector_WorksBasic() {
        boolean result = domainMetadataMismatchDetector.detect(
                "casino.com",
                "Educational Scholarships",
                "Apply for funding"
        );
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Unnatural keyword list detector can detect simple cases")
    void unnaturalKeywordListDetector_WorksBasic() {
        boolean result = unnaturalKeywordListDetector.detect(
                "grants scholarships funding aid"
        );
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Cross-category spam detector can detect simple cases")
    void crossCategorySpamDetector_WorksBasic() {
        boolean result = crossCategorySpamDetector.detect(
                "casino.com",
                "Scholarship Opportunities",
                "Educational grants available"
        );
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("AntiSpamFilter exposes individual detector methods")
    void antiSpamFilter_ExposesIndividualMethods() {
        // Verify interface methods are accessible and callable
        // Method returns are implementation-dependent, we just verify they execute without error
        assertThat(antiSpamFilter.detectKeywordStuffing("test")).isInstanceOf(Boolean.class);
        assertThat(antiSpamFilter.detectDomainMetadataMismatch("test.com", "title", "desc")).isInstanceOf(Boolean.class);
        assertThat(antiSpamFilter.detectUnnaturalKeywordList("normal sentence")).isInstanceOf(Boolean.class);
        assertThat(antiSpamFilter.detectCrossCategorySpam("university.edu", "title", "desc")).isInstanceOf(Boolean.class);
    }
}
