# Feature 005: Enhanced Taxonomy & Basic Scheduler - Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Add multi-dimensional funding taxonomy (66 enum values across 6 new enums) and basic scheduler with fixed daily schedule to enable precise, targeted funding discovery queries.

**Architecture:** Three-layer approach - (1) Domain enums for taxonomy, (2) Enhanced CategoryMapper for multi-dimensional keyword generation, (3) New scheduler module with DailyScheduleService for batch orchestration. Backward compatible with Feature 004.

**Tech Stack:** Java 25, Spring Boot 3.5.6, Spring Data JDBC, PostgreSQL 16, Lombok, JUnit 5, Mockito

---

## Prerequisites

- Git worktree: `.worktrees/005-enhanced-taxonomy` ✅ COMPLETE
- Branch: `005-enhanced-taxonomy` ✅ COMPLETE
- Baseline tests: 421 tests passing ✅ COMPLETE
- Design document: `docs/plans/2025-11-04-feature-005-enhanced-taxonomy-design.md` ✅ COMPLETE

---

## Task 1: Add FundingSourceType Enum

**Files:**
- Create: `northstar-domain/src/main/java/com/northstar/funding/domain/FundingSourceType.java`
- Create: `northstar-domain/src/test/java/com/northstar/funding/domain/FundingSourceTypeTest.java`

**Step 1: Write the failing test**

```bash
cd northstar-domain
mkdir -p src/test/java/com/northstar/funding/domain
```

Create `src/test/java/com/northstar/funding/domain/FundingSourceTypeTest.java`:

```java
package com.northstar.funding.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FundingSourceTypeTest {

    @Test
    void shouldHaveTwelveValues() {
        assertThat(FundingSourceType.values()).hasSize(12);
    }

    @Test
    void shouldContainGovernmentTypes() {
        assertThat(FundingSourceType.GOVERNMENT_NATIONAL).isNotNull();
        assertThat(FundingSourceType.GOVERNMENT_EU).isNotNull();
        assertThat(FundingSourceType.GOVERNMENT_REGIONAL).isNotNull();
    }

    @Test
    void shouldContainFoundationTypes() {
        assertThat(FundingSourceType.PRIVATE_FOUNDATION).isNotNull();
        assertThat(FundingSourceType.CORPORATE_FOUNDATION).isNotNull();
        assertThat(FundingSourceType.COMMUNITY_FOUNDATION).isNotNull();
        assertThat(FundingSourceType.RELIGIOUS_FOUNDATION).isNotNull();
    }

    @Test
    void shouldContainInternationalTypes() {
        assertThat(FundingSourceType.BILATERAL_AID).isNotNull();
        assertThat(FundingSourceType.MULTILATERAL_ORG).isNotNull();
    }

    @Test
    void shouldContainOtherTypes() {
        assertThat(FundingSourceType.EDUCATION_ASSOCIATION).isNotNull();
        assertThat(FundingSourceType.CROWDFUNDING_PLATFORM).isNotNull();
        assertThat(FundingSourceType.CREDIT_UNION_OR_BANK).isNotNull();
    }
}
```

**Step 2: Run test to verify it fails**

```bash
cd northstar-domain
mvn test -Dtest=FundingSourceTypeTest
```

Expected: FAIL with compilation error (class does not exist)

**Step 3: Write minimal implementation**

Create `src/main/java/com/northstar/funding/domain/FundingSourceType.java`:

```java
package com.northstar.funding.domain;

/**
 * Classifies WHO provides the funding.
 *
 * This enum represents different types of organizations and institutions
 * that offer funding opportunities for educational projects.
 */
public enum FundingSourceType {

    /** National government ministries and agencies */
    GOVERNMENT_NATIONAL,

    /** European Union Commission and institutions */
    GOVERNMENT_EU,

    /** Regional, municipal, and local governments */
    GOVERNMENT_REGIONAL,

    /** Private philanthropic foundations */
    PRIVATE_FOUNDATION,

    /** Corporate CSR and charitable foundations */
    CORPORATE_FOUNDATION,

    /** Bilateral aid agencies (USAID, GIZ, British Council, etc.) */
    BILATERAL_AID,

    /** Multilateral organizations (World Bank, UN agencies, etc.) */
    MULTILATERAL_ORG,

    /** Community-based local foundations */
    COMMUNITY_FOUNDATION,

    /** Professional education associations and networks */
    EDUCATION_ASSOCIATION,

    /** Crowdfunding platforms for education projects */
    CROWDFUNDING_PLATFORM,

    /** Religious and faith-based funding organizations */
    RELIGIOUS_FOUNDATION,

    /** Financial institutions offering education loans and grants */
    CREDIT_UNION_OR_BANK
}
```

**Step 4: Run test to verify it passes**

```bash
cd northstar-domain
mvn test -Dtest=FundingSourceTypeTest
```

Expected: PASS (5 tests, 0 failures)

**Step 5: Commit**

```bash
git add northstar-domain/src/main/java/com/northstar/funding/domain/FundingSourceType.java
git add northstar-domain/src/test/java/com/northstar/funding/domain/FundingSourceTypeTest.java
git commit -m "feat: Add FundingSourceType enum with 12 values"
```

---

## Task 2: Add FundingMechanism Enum

**Files:**
- Create: `northstar-domain/src/main/java/com/northstar/funding/domain/FundingMechanism.java`
- Create: `northstar-domain/src/test/java/com/northstar/funding/domain/FundingMechanismTest.java`

**Step 1: Write the failing test**

Create `northstar-domain/src/test/java/com/northstar/funding/domain/FundingMechanismTest.java`:

```java
package com.northstar.funding.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FundingMechanismTest {

    @Test
    void shouldHaveEightValues() {
        assertThat(FundingMechanism.values()).hasSize(8);
    }

    @Test
    void shouldContainGrantTypes() {
        assertThat(FundingMechanism.GRANT).isNotNull();
        assertThat(FundingMechanism.MATCHING_GRANT).isNotNull();
    }

    @Test
    void shouldContainIndividualFundingTypes() {
        assertThat(FundingMechanism.SCHOLARSHIP).isNotNull();
        assertThat(FundingMechanism.FELLOWSHIP).isNotNull();
    }

    @Test
    void shouldContainRepayableMechanisms() {
        assertThat(FundingMechanism.LOAN).isNotNull();
    }

    @Test
    void shouldContainOtherMechanisms() {
        assertThat(FundingMechanism.PRIZE_AWARD).isNotNull();
        assertThat(FundingMechanism.IN_KIND_DONATION).isNotNull();
        assertThat(FundingMechanism.SUBSIDY).isNotNull();
    }
}
```

**Step 2: Run test to verify it fails**

```bash
cd northstar-domain
mvn test -Dtest=FundingMechanismTest
```

Expected: FAIL (compilation error)

**Step 3: Write minimal implementation**

Create `northstar-domain/src/main/java/com/northstar/funding/domain/FundingMechanism.java`:

```java
package com.northstar.funding.domain;

/**
 * Classifies HOW funding is distributed.
 *
 * This enum represents different mechanisms through which funding
 * opportunities are provided to recipients.
 */
public enum FundingMechanism {

    /** Non-repayable grant funding */
    GRANT,

    /** Repayable loan with various terms */
    LOAN,

    /** Individual student scholarship */
    SCHOLARSHIP,

    /** Research or professional fellowship */
    FELLOWSHIP,

    /** Grant requiring matching funds from recipient */
    MATCHING_GRANT,

    /** Competitive prize or award */
    PRIZE_AWARD,

    /** In-kind donation of equipment, materials, or services */
    IN_KIND_DONATION,

    /** Government subsidy or tax benefit */
    SUBSIDY
}
```

**Step 4: Run test to verify it passes**

```bash
cd northstar-domain
mvn test -Dtest=FundingMechanismTest
```

Expected: PASS (5 tests, 0 failures)

**Step 5: Commit**

```bash
git add northstar-domain/src/main/java/com/northstar/funding/domain/FundingMechanism.java
git add northstar-domain/src/test/java/com/northstar/funding/domain/FundingMechanismTest.java
git commit -m "feat: Add FundingMechanism enum with 8 values"
```

---

## Task 3: Add ProjectScale Enum with Amount Ranges

**Files:**
- Create: `northstar-domain/src/main/java/com/northstar/funding/domain/ProjectScale.java`
- Create: `northstar-domain/src/test/java/com/northstar/funding/domain/ProjectScaleTest.java`

**Step 1: Write the failing test**

Create `northstar-domain/src/test/java/com/northstar/funding/domain/ProjectScaleTest.java`:

```java
package com.northstar.funding.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class ProjectScaleTest {

    @Test
    void shouldHaveFiveValues() {
        assertThat(ProjectScale.values()).hasSize(5);
    }

    @Test
    void microShouldHaveCorrectRange() {
        ProjectScale micro = ProjectScale.MICRO;
        assertThat(micro.getDisplayName()).isEqualTo("< €5k");
        assertThat(micro.getMinAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(micro.getMaxAmount()).isEqualByComparingTo(new BigDecimal("5000"));
    }

    @Test
    void smallShouldHaveCorrectRange() {
        ProjectScale small = ProjectScale.SMALL;
        assertThat(small.getDisplayName()).isEqualTo("€5k - €50k");
        assertThat(small.getMinAmount()).isEqualByComparingTo(new BigDecimal("5000"));
        assertThat(small.getMaxAmount()).isEqualByComparingTo(new BigDecimal("50000"));
    }

    @Test
    void mediumShouldHaveCorrectRange() {
        ProjectScale medium = ProjectScale.MEDIUM;
        assertThat(medium.getDisplayName()).isEqualTo("€50k - €250k");
        assertThat(medium.getMinAmount()).isEqualByComparingTo(new BigDecimal("50000"));
        assertThat(medium.getMaxAmount()).isEqualByComparingTo(new BigDecimal("250000"));
    }

    @Test
    void largeShouldHaveCorrectRange() {
        ProjectScale large = ProjectScale.LARGE;
        assertThat(large.getDisplayName()).isEqualTo("€250k - €1M");
        assertThat(large.getMinAmount()).isEqualByComparingTo(new BigDecimal("250000"));
        assertThat(large.getMaxAmount()).isEqualByComparingTo(new BigDecimal("1000000"));
    }

    @Test
    void megaShouldHaveNoMaxAmount() {
        ProjectScale mega = ProjectScale.MEGA;
        assertThat(mega.getDisplayName()).isEqualTo("> €1M");
        assertThat(mega.getMinAmount()).isEqualByComparingTo(new BigDecimal("1000000"));
        assertThat(mega.getMaxAmount()).isNull();
    }

    @Test
    void shouldOrderByIncreasingSize() {
        ProjectScale[] scales = ProjectScale.values();
        assertThat(scales[0]).isEqualTo(ProjectScale.MICRO);
        assertThat(scales[1]).isEqualTo(ProjectScale.SMALL);
        assertThat(scales[2]).isEqualTo(ProjectScale.MEDIUM);
        assertThat(scales[3]).isEqualTo(ProjectScale.LARGE);
        assertThat(scales[4]).isEqualTo(ProjectScale.MEGA);
    }
}
```

**Step 2: Run test to verify it fails**

```bash
cd northstar-domain
mvn test -Dtest=ProjectScaleTest
```

Expected: FAIL (compilation error)

**Step 3: Write minimal implementation**

Create `northstar-domain/src/main/java/com/northstar/funding/domain/ProjectScale.java`:

```java
package com.northstar.funding.domain;

import java.math.BigDecimal;

/**
 * Classifies funding amount ranges (Euro-denominated).
 *
 * This enum represents typical project funding scales with min/max
 * amount boundaries for filtering and matching funding opportunities.
 */
public enum ProjectScale {

    MICRO("< €5k", BigDecimal.ZERO, new BigDecimal("5000")),
    SMALL("€5k - €50k", new BigDecimal("5000"), new BigDecimal("50000")),
    MEDIUM("€50k - €250k", new BigDecimal("50000"), new BigDecimal("250000")),
    LARGE("€250k - €1M", new BigDecimal("250000"), new BigDecimal("1000000")),
    MEGA("> €1M", new BigDecimal("1000000"), null);

    private final String displayName;
    private final BigDecimal minAmount;
    private final BigDecimal maxAmount;  // null for MEGA (unlimited)

    ProjectScale(String displayName, BigDecimal minAmount, BigDecimal maxAmount) {
        this.displayName = displayName;
        this.minAmount = minAmount;
        this.maxAmount = maxAmount;
    }

    public String getDisplayName() {
        return displayName;
    }

    public BigDecimal getMinAmount() {
        return minAmount;
    }

    public BigDecimal getMaxAmount() {
        return maxAmount;
    }
}
```

**Step 4: Run test to verify it passes**

```bash
cd northstar-domain
mvn test -Dtest=ProjectScaleTest
```

Expected: PASS (7 tests, 0 failures)

**Step 5: Commit**

```bash
git add northstar-domain/src/main/java/com/northstar/funding/domain/ProjectScale.java
git add northstar-domain/src/test/java/com/northstar/funding/domain/ProjectScaleTest.java
git commit -m "feat: Add ProjectScale enum with amount ranges"
```

---

## Task 4: Add BeneficiaryPopulation Enum

**Files:**
- Create: `northstar-domain/src/main/java/com/northstar/funding/domain/BeneficiaryPopulation.java`
- Create: `northstar-domain/src/test/java/com/northstar/funding/domain/BeneficiaryPopulationTest.java`

**Step 1: Write the failing test**

Create `northstar-domain/src/test/java/com/northstar/funding/domain/BeneficiaryPopulationTest.java`:

```java
package com.northstar.funding.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BeneficiaryPopulationTest {

    @Test
    void shouldHaveEighteenValues() {
        assertThat(BeneficiaryPopulation.values()).hasSize(18);
    }

    @Test
    void shouldContainSocioeconomicCategories() {
        assertThat(BeneficiaryPopulation.LOW_INCOME_FAMILIES).isNotNull();
        assertThat(BeneficiaryPopulation.RURAL_COMMUNITIES).isNotNull();
        assertThat(BeneficiaryPopulation.ETHNIC_MINORITIES).isNotNull();
    }

    @Test
    void shouldContainGenderCategories() {
        assertThat(BeneficiaryPopulation.GIRLS_WOMEN).isNotNull();
        assertThat(BeneficiaryPopulation.LGBTQ_PLUS).isNotNull();
    }

    @Test
    void shouldContainAgeRangeCategories() {
        assertThat(BeneficiaryPopulation.EARLY_CHILDHOOD_0_5).isNotNull();
        assertThat(BeneficiaryPopulation.CHILDREN_AGES_4_12).isNotNull();
        assertThat(BeneficiaryPopulation.ADOLESCENTS_AGES_13_18).isNotNull();
        assertThat(BeneficiaryPopulation.ADULTS_LIFELONG_LEARNING).isNotNull();
        assertThat(BeneficiaryPopulation.ELDERLY).isNotNull();
    }

    @Test
    void shouldContainEducationalAccessCategories() {
        assertThat(BeneficiaryPopulation.FIRST_GENERATION_STUDENTS).isNotNull();
        assertThat(BeneficiaryPopulation.AT_RISK_YOUTH).isNotNull();
        assertThat(BeneficiaryPopulation.LANGUAGE_MINORITIES).isNotNull();
    }

    @Test
    void shouldContainSpecialNeedsCategories() {
        assertThat(BeneficiaryPopulation.PEOPLE_WITH_DISABILITIES).isNotNull();
        assertThat(BeneficiaryPopulation.REFUGEES_IMMIGRANTS).isNotNull();
        assertThat(BeneficiaryPopulation.VETERANS).isNotNull();
    }

    @Test
    void shouldContainRoleBasedCategories() {
        assertThat(BeneficiaryPopulation.EDUCATORS_TEACHERS).isNotNull();
        assertThat(BeneficiaryPopulation.GENERAL_POPULATION).isNotNull();
    }
}
```

**Step 2: Run test to verify it fails**

```bash
cd northstar-domain
mvn test -Dtest=BeneficiaryPopulationTest
```

Expected: FAIL (compilation error)

**Step 3: Write minimal implementation**

Create `northstar-domain/src/main/java/com/northstar/funding/domain/BeneficiaryPopulation.java`:

```java
package com.northstar.funding.domain;

/**
 * Classifies WHO benefits from the funding.
 *
 * This enum represents different target populations that funding
 * opportunities aim to serve or support.
 */
public enum BeneficiaryPopulation {

    /** Low-income families and economically disadvantaged groups */
    LOW_INCOME_FAMILIES,

    /** Rural and remote community populations */
    RURAL_COMMUNITIES,

    /** Ethnic minority groups and underrepresented populations */
    ETHNIC_MINORITIES,

    /** Girls and women in education */
    GIRLS_WOMEN,

    /** Children ages 4-12 (primary school age) */
    CHILDREN_AGES_4_12,

    /** Adolescents ages 13-18 (secondary school age) */
    ADOLESCENTS_AGES_13_18,

    /** First-generation college/university students */
    FIRST_GENERATION_STUDENTS,

    /** At-risk youth and vulnerable young people */
    AT_RISK_YOUTH,

    /** People with disabilities and special educational needs */
    PEOPLE_WITH_DISABILITIES,

    /** Refugees, immigrants, and displaced populations */
    REFUGEES_IMMIGRANTS,

    /** Language minorities and non-native speakers */
    LANGUAGE_MINORITIES,

    /** Early childhood ages 0-5 (pre-school) */
    EARLY_CHILDHOOD_0_5,

    /** Adults pursuing lifelong learning and continuing education */
    ADULTS_LIFELONG_LEARNING,

    /** Teachers, educators, and educational professionals */
    EDUCATORS_TEACHERS,

    /** General population (no specific targeting) */
    GENERAL_POPULATION,

    /** LGBTQ+ individuals and communities */
    LGBTQ_PLUS,

    /** Military veterans and their families */
    VETERANS,

    /** Elderly and senior citizens */
    ELDERLY
}
```

**Step 4: Run test to verify it passes**

```bash
cd northstar-domain
mvn test -Dtest=BeneficiaryPopulationTest
```

Expected: PASS (7 tests, 0 failures)

**Step 5: Commit**

```bash
git add northstar-domain/src/main/java/com/northstar/funding/domain/BeneficiaryPopulation.java
git add northstar-domain/src/test/java/com/northstar/funding/domain/BeneficiaryPopulationTest.java
git commit -m "feat: Add BeneficiaryPopulation enum with 18 values"
```

---

## Task 5: Add RecipientOrganizationType Enum

**Files:**
- Create: `northstar-domain/src/main/java/com/northstar/funding/domain/RecipientOrganizationType.java`
- Create: `northstar-domain/src/test/java/com/northstar/funding/domain/RecipientOrganizationTypeTest.java`

**Step 1: Write the failing test**

Create `northstar-domain/src/test/java/com/northstar/funding/domain/RecipientOrganizationTypeTest.java`:

```java
package com.northstar.funding.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RecipientOrganizationTypeTest {

    @Test
    void shouldHaveFourteenValues() {
        assertThat(RecipientOrganizationType.values()).hasSize(14);
    }

    @Test
    void shouldContainLanguageSchoolTypes() {
        assertThat(RecipientOrganizationType.PRIVATE_LANGUAGE_SCHOOL).isNotNull();
    }

    @Test
    void shouldContainK12SchoolTypes() {
        assertThat(RecipientOrganizationType.K12_PRIVATE_SCHOOL).isNotNull();
        assertThat(RecipientOrganizationType.K12_PUBLIC_SCHOOL).isNotNull();
        assertThat(RecipientOrganizationType.PRESCHOOL_EARLY_CHILDHOOD).isNotNull();
    }

    @Test
    void shouldContainNGOTypes() {
        assertThat(RecipientOrganizationType.NGO_EDUCATION_FOCUSED).isNotNull();
        assertThat(RecipientOrganizationType.NGO_SOCIAL_SERVICES).isNotNull();
    }

    @Test
    void shouldContainHigherEducationTypes() {
        assertThat(RecipientOrganizationType.UNIVERSITY_PUBLIC).isNotNull();
    }

    @Test
    void shouldContainIndividualTypes() {
        assertThat(RecipientOrganizationType.INDIVIDUAL_EDUCATOR).isNotNull();
        assertThat(RecipientOrganizationType.INDIVIDUAL_STUDENT).isNotNull();
    }

    @Test
    void shouldContainOtherTypes() {
        assertThat(RecipientOrganizationType.EXAMINATION_CENTER).isNotNull();
        assertThat(RecipientOrganizationType.FOR_PROFIT_EDUCATION).isNotNull();
        assertThat(RecipientOrganizationType.MUNICIPALITY).isNotNull();
        assertThat(RecipientOrganizationType.RESEARCH_INSTITUTE).isNotNull();
        assertThat(RecipientOrganizationType.LIBRARY_OR_CULTURAL_CENTER).isNotNull();
    }
}
```

**Step 2: Run test to verify it fails**

```bash
cd northstar-domain
mvn test -Dtest=RecipientOrganizationTypeTest
```

Expected: FAIL (compilation error)

**Step 3: Write minimal implementation**

Create `northstar-domain/src/main/java/com/northstar/funding/domain/RecipientOrganizationType.java`:

```java
package com.northstar.funding.domain;

/**
 * Classifies WHAT TYPE of organization receives funding.
 *
 * This enum represents different organizational categories that
 * can receive funding opportunities.
 */
public enum RecipientOrganizationType {

    /** Private language schools and training centers */
    PRIVATE_LANGUAGE_SCHOOL,

    /** K-12 private schools */
    K12_PRIVATE_SCHOOL,

    /** K-12 public schools */
    K12_PUBLIC_SCHOOL,

    /** Preschool and early childhood centers */
    PRESCHOOL_EARLY_CHILDHOOD,

    /** Examination and testing centers */
    EXAMINATION_CENTER,

    /** Education-focused NGOs and nonprofits */
    NGO_EDUCATION_FOCUSED,

    /** Social services NGOs with education programs */
    NGO_SOCIAL_SERVICES,

    /** For-profit education companies */
    FOR_PROFIT_EDUCATION,

    /** Public universities and colleges */
    UNIVERSITY_PUBLIC,

    /** Municipal and local government entities */
    MUNICIPALITY,

    /** Individual educators and teachers */
    INDIVIDUAL_EDUCATOR,

    /** Individual students and learners */
    INDIVIDUAL_STUDENT,

    /** Research institutes and think tanks */
    RESEARCH_INSTITUTE,

    /** Libraries, museums, and cultural centers */
    LIBRARY_OR_CULTURAL_CENTER
}
```

**Step 4: Run test to verify it passes**

```bash
cd northstar-domain
mvn test -Dtest=RecipientOrganizationTypeTest
```

Expected: PASS (7 tests, 0 failures)

**Step 5: Commit**

```bash
git add northstar-domain/src/main/java/com/northstar/funding/domain/RecipientOrganizationType.java
git add northstar-domain/src/test/java/com/northstar/funding/domain/RecipientOrganizationTypeTest.java
git commit -m "feat: Add RecipientOrganizationType enum with 14 values"
```

---

## Task 6: Add QueryLanguage Enum with ISO Codes

**Files:**
- Create: `northstar-domain/src/main/java/com/northstar/funding/domain/QueryLanguage.java`
- Create: `northstar-domain/src/test/java/com/northstar/funding/domain/QueryLanguageTest.java`

**Step 1: Write the failing test**

Create `northstar-domain/src/test/java/com/northstar/funding/domain/QueryLanguageTest.java`:

```java
package com.northstar.funding.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class QueryLanguageTest {

    @Test
    void shouldHaveNineValues() {
        assertThat(QueryLanguage.values()).hasSize(9);
    }

    @Test
    void bulgarianShouldHaveCorrectCodes() {
        QueryLanguage bg = QueryLanguage.BULGARIAN;
        assertThat(bg.getLanguageCode()).isEqualTo("bg");
        assertThat(bg.getNativeName()).isEqualTo("български");
    }

    @Test
    void englishShouldHaveCorrectCodes() {
        QueryLanguage en = QueryLanguage.ENGLISH;
        assertThat(en.getLanguageCode()).isEqualTo("en");
        assertThat(en.getNativeName()).isEqualTo("English");
    }

    @Test
    void germanShouldHaveCorrectCodes() {
        QueryLanguage de = QueryLanguage.GERMAN;
        assertThat(de.getLanguageCode()).isEqualTo("de");
        assertThat(de.getNativeName()).isEqualTo("Deutsch");
    }

    @Test
    void romanianShouldHaveCorrectCodes() {
        QueryLanguage ro = QueryLanguage.ROMANIAN;
        assertThat(ro.getLanguageCode()).isEqualTo("ro");
        assertThat(ro.getNativeName()).isEqualTo("română");
    }

    @Test
    void frenchShouldHaveCorrectCodes() {
        QueryLanguage fr = QueryLanguage.FRENCH;
        assertThat(fr.getLanguageCode()).isEqualTo("fr");
        assertThat(fr.getNativeName()).isEqualTo("français");
    }

    @Test
    void russianShouldHaveCorrectCodes() {
        QueryLanguage ru = QueryLanguage.RUSSIAN;
        assertThat(ru.getLanguageCode()).isEqualTo("ru");
        assertThat(ru.getNativeName()).isEqualTo("русский");
    }

    @Test
    void greekShouldHaveCorrectCodes() {
        QueryLanguage el = QueryLanguage.GREEK;
        assertThat(el.getLanguageCode()).isEqualTo("el");
        assertThat(el.getNativeName()).isEqualTo("ελληνικά");
    }

    @Test
    void turkishShouldHaveCorrectCodes() {
        QueryLanguage tr = QueryLanguage.TURKISH;
        assertThat(tr.getLanguageCode()).isEqualTo("tr");
        assertThat(tr.getNativeName()).isEqualTo("Türkçe");
    }

    @Test
    void serbianShouldHaveCorrectCodes() {
        QueryLanguage sr = QueryLanguage.SERBIAN;
        assertThat(sr.getLanguageCode()).isEqualTo("sr");
        assertThat(sr.getNativeName()).isEqualTo("српски");
    }

    @Test
    void allLanguageCodesShouldBeTwoCharacters() {
        for (QueryLanguage lang : QueryLanguage.values()) {
            assertThat(lang.getLanguageCode())
                .hasSize(2)
                .isLowerCase();
        }
    }
}
```

**Step 2: Run test to verify it fails**

```bash
cd northstar-domain
mvn test -Dtest=QueryLanguageTest
```

Expected: FAIL (compilation error)

**Step 3: Write minimal implementation**

Create `northstar-domain/src/main/java/com/northstar/funding/domain/QueryLanguage.java`:

```java
package com.northstar.funding.domain;

/**
 * Defines languages for query generation (future translation support).
 *
 * This enum represents languages supported for generating funding search queries.
 * Each language includes ISO 639-1 code and native name for future translation features.
 *
 * NOTE: Translation service NOT implemented yet - enum structure only.
 */
public enum QueryLanguage {

    BULGARIAN("bg", "български"),
    ENGLISH("en", "English"),
    GERMAN("de", "Deutsch"),
    ROMANIAN("ro", "română"),
    FRENCH("fr", "français"),
    RUSSIAN("ru", "русский"),
    GREEK("el", "ελληνικά"),
    TURKISH("tr", "Türkçe"),
    SERBIAN("sr", "српски");

    private final String languageCode;    // ISO 639-1 code
    private final String nativeName;      // Display name in native script

    QueryLanguage(String languageCode, String nativeName) {
        this.languageCode = languageCode;
        this.nativeName = nativeName;
    }

    public String getLanguageCode() {
        return languageCode;
    }

    public String getNativeName() {
        return nativeName;
    }
}
```

**Step 4: Run test to verify it passes**

```bash
cd northstar-domain
mvn test -Dtest=QueryLanguageTest
```

Expected: PASS (11 tests, 0 failures)

**Step 5: Commit**

```bash
git add northstar-domain/src/main/java/com/northstar/funding/domain/QueryLanguage.java
git add northstar-domain/src/test/java/com/northstar/funding/domain/QueryLanguageTest.java
git commit -m "feat: Add QueryLanguage enum with ISO codes and native names"
```

---

## Task 7: Extend FundingSearchCategory with 5 New Values

**Files:**
- Modify: `northstar-domain/src/main/java/com/northstar/funding/domain/FundingSearchCategory.java`
- Modify: `northstar-query-generation/src/test/java/com/northstar/funding/querygeneration/service/CategoryMapperTest.java`

**Step 1: Write the failing test**

Add to existing test file `northstar-query-generation/src/test/java/com/northstar/funding/querygeneration/service/CategoryMapperTest.java`:

```java
@Test
void shouldReturnKeywordsForEarlyChildhoodEducation() {
    List<String> keywords = categoryMapper.getKeywords(FundingSearchCategory.EARLY_CHILDHOOD_EDUCATION);
    assertThat(keywords)
        .isNotEmpty()
        .contains("early childhood", "preschool", "kindergarten");
}

@Test
void shouldReturnKeywordsForAdultEducation() {
    List<String> keywords = categoryMapper.getKeywords(FundingSearchCategory.ADULT_EDUCATION);
    assertThat(keywords)
        .isNotEmpty()
        .contains("adult education", "lifelong learning", "continuing education");
}

@Test
void shouldReturnKeywordsForVocationalTraining() {
    List<String> keywords = categoryMapper.getKeywords(FundingSearchCategory.VOCATIONAL_TRAINING);
    assertThat(keywords)
        .isNotEmpty()
        .contains("vocational", "technical training", "skills development");
}

@Test
void shouldReturnKeywordsForEducationalTechnology() {
    List<String> keywords = categoryMapper.getKeywords(FundingSearchCategory.EDUCATIONAL_TECHNOLOGY);
    assertThat(keywords)
        .isNotEmpty()
        .contains("edtech", "educational technology", "digital learning");
}

@Test
void shouldReturnKeywordsForArtsCulture() {
    List<String> keywords = categoryMapper.getKeywords(FundingSearchCategory.ARTS_CULTURE);
    assertThat(keywords)
        .isNotEmpty()
        .contains("arts", "culture", "creative education");
}
```

**Step 2: Run test to verify it fails**

```bash
cd northstar-query-generation
mvn test -Dtest=CategoryMapperTest
```

Expected: FAIL (compilation errors for new enum values)

**Step 3: Add new enum values**

Edit `northstar-domain/src/main/java/com/northstar/funding/domain/FundingSearchCategory.java`:

Add these 5 new values to the existing enum (after the existing 25 values):

```java
    /** Early childhood education (ages 0-5) funding */
    EARLY_CHILDHOOD_EDUCATION,

    /** Adult education and lifelong learning funding */
    ADULT_EDUCATION,

    /** Vocational and technical training funding */
    VOCATIONAL_TRAINING,

    /** Educational technology and digital learning funding */
    EDUCATIONAL_TECHNOLOGY,

    /** Arts and cultural education funding */
    ARTS_CULTURE
```

**Step 4: Add keyword mappings to CategoryMapper**

Edit `northstar-query-generation/src/main/java/com/northstar/funding/querygeneration/service/CategoryMapper.java`:

Add these cases to the switch statement in `getKeywords()` method:

```java
            case EARLY_CHILDHOOD_EDUCATION -> List.of(
                "early childhood education", "preschool", "kindergarten",
                "pre-K", "nursery", "childcare", "ages 0-5"
            );
            case ADULT_EDUCATION -> List.of(
                "adult education", "lifelong learning", "continuing education",
                "adult learning", "mature students", "non-traditional students"
            );
            case VOCATIONAL_TRAINING -> List.of(
                "vocational training", "technical training", "skills development",
                "vocational education", "career training", "trade skills"
            );
            case EDUCATIONAL_TECHNOLOGY -> List.of(
                "educational technology", "edtech", "digital learning",
                "e-learning", "online education", "learning technology"
            );
            case ARTS_CULTURE -> List.of(
                "arts education", "cultural education", "creative arts",
                "performing arts", "visual arts", "arts integration"
            );
```

**Step 5: Run test to verify it passes**

```bash
cd northstar-query-generation
mvn test -Dtest=CategoryMapperTest
```

Expected: PASS (all tests including 5 new tests)

**Step 6: Commit**

```bash
git add northstar-domain/src/main/java/com/northstar/funding/domain/FundingSearchCategory.java
git add northstar-query-generation/src/main/java/com/northstar/funding/querygeneration/service/CategoryMapper.java
git add northstar-query-generation/src/test/java/com/northstar/funding/querygeneration/service/CategoryMapperTest.java
git commit -m "feat: Add 5 new FundingSearchCategory values with keyword mappings"
```

---

## Task 8: Enhance QueryGenerationRequest with Multi-Dimensional Fields

**Files:**
- Modify: `northstar-query-generation/src/main/java/com/northstar/funding/querygeneration/dto/QueryGenerationRequest.java`

**Step 1: Add new optional fields to QueryGenerationRequest**

Edit `northstar-query-generation/src/main/java/com/northstar/funding/querygeneration/dto/QueryGenerationRequest.java`:

Add these fields after the existing fields (before the closing brace):

```java
    // ========================================
    // OPTIONAL FIELDS (Feature 005 - NEW)
    // ========================================

    /**
     * WHO provides funding (optional).
     * Adds source-specific keywords to queries.
     */
    private FundingSourceType sourceType;

    /**
     * HOW funding is distributed (optional).
     * Adds mechanism-specific keywords to queries.
     */
    private FundingMechanism mechanism;

    /**
     * Funding amount range (optional).
     * Adds scale-specific keywords to queries.
     */
    private ProjectScale projectScale;

    /**
     * WHO benefits from funding (optional).
     * Can specify multiple beneficiary populations.
     * Adds beneficiary-specific keywords to queries.
     */
    private Set<BeneficiaryPopulation> beneficiaries;

    /**
     * WHAT TYPE of organization receives funding (optional).
     * Adds recipient-specific keywords to queries.
     */
    private RecipientOrganizationType recipientType;

    /**
     * User's preferred language (optional).
     * For future translation service integration.
     * NOTE: Translation NOT implemented yet.
     */
    private QueryLanguage userLanguage;

    /**
     * Languages to search in (optional).
     * For future multi-language query generation.
     * NOTE: Translation NOT implemented yet.
     */
    private Set<QueryLanguage> searchLanguages;
```

**Step 2: Add imports**

Add these imports at the top of the file:

```java
import com.northstar.funding.domain.FundingSourceType;
import com.northstar.funding.domain.FundingMechanism;
import com.northstar.funding.domain.ProjectScale;
import com.northstar.funding.domain.BeneficiaryPopulation;
import com.northstar.funding.domain.RecipientOrganizationType;
import com.northstar.funding.domain.QueryLanguage;
```

**Step 3: Verify compilation**

```bash
cd northstar-query-generation
mvn clean compile
```

Expected: SUCCESS

**Step 4: Run existing tests to ensure backward compatibility**

```bash
cd northstar-query-generation
mvn test
```

Expected: All existing tests PASS (no changes to test logic needed)

**Step 5: Commit**

```bash
git add northstar-query-generation/src/main/java/com/northstar/funding/querygeneration/dto/QueryGenerationRequest.java
git commit -m "feat: Add multi-dimensional fields to QueryGenerationRequest (backward compatible)"
```

---

## Task 9: Enhance CategoryMapper with Multi-Dimensional Keyword Generation

**Files:**
- Modify: `northstar-query-generation/src/main/java/com/northstar/funding/querygeneration/service/CategoryMapper.java`
- Create: `northstar-query-generation/src/test/java/com/northstar/funding/querygeneration/service/MultiDimensionalCategoryMapperTest.java`

**Step 1: Write failing tests for multi-dimensional mapper**

Create `northstar-query-generation/src/test/java/com/northstar/funding/querygeneration/service/MultiDimensionalCategoryMapperTest.java`:

```java
package com.northstar.funding.querygeneration.service;

import com.northstar.funding.domain.*;
import com.northstar.funding.querygeneration.dto.QueryGenerationRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class MultiDimensionalCategoryMapperTest {

    private CategoryMapper categoryMapper;

    @BeforeEach
    void setUp() {
        categoryMapper = new CategoryMapper();
    }

    @Test
    void shouldReturnOnlyCategoryKeywordsWhenNoOptionalDimensions() {
        QueryGenerationRequest request = QueryGenerationRequest.builder()
            .category(FundingSearchCategory.STEM_EDUCATION)
            .build();

        List<String> keywords = categoryMapper.getKeywords(request);

        assertThat(keywords)
            .isNotEmpty()
            .contains("STEM", "science", "technology")
            .doesNotContain("government", "loan", "school");
    }

    @Test
    void shouldCombineKeywordsFromMultipleDimensions() {
        QueryGenerationRequest request = QueryGenerationRequest.builder()
            .category(FundingSearchCategory.INFRASTRUCTURE_FACILITIES)
            .sourceType(FundingSourceType.GOVERNMENT_REGIONAL)
            .mechanism(FundingMechanism.LOAN)
            .projectScale(ProjectScale.MEDIUM)
            .recipientType(RecipientOrganizationType.PRIVATE_LANGUAGE_SCHOOL)
            .build();

        List<String> keywords = categoryMapper.getKeywords(request);

        // Should contain keywords from ALL dimensions
        assertThat(keywords)
            .contains("infrastructure", "building")           // category
            .contains("government", "regional", "municipal")  // sourceType
            .contains("loan", "financing")                    // mechanism
            .contains("medium scale", "50k-250k")             // projectScale
            .contains("language school", "private education"); // recipientType
    }

    @Test
    void shouldHandleMultipleBeneficiaries() {
        QueryGenerationRequest request = QueryGenerationRequest.builder()
            .category(FundingSearchCategory.SCHOLARSHIPS_FINANCIAL_AID)
            .beneficiaries(Set.of(
                BeneficiaryPopulation.LOW_INCOME_FAMILIES,
                BeneficiaryPopulation.FIRST_GENERATION_STUDENTS
            ))
            .build();

        List<String> keywords = categoryMapper.getKeywords(request);

        assertThat(keywords)
            .contains("scholarship", "financial aid")         // category
            .contains("low income", "economically disadvantaged") // beneficiary 1
            .contains("first generation", "family access");   // beneficiary 2
    }

    @Test
    void shouldDeduplicateKeywords() {
        QueryGenerationRequest request = QueryGenerationRequest.builder()
            .category(FundingSearchCategory.SCHOLARSHIPS_FINANCIAL_AID)
            .mechanism(FundingMechanism.SCHOLARSHIP)
            .build();

        List<String> keywords = categoryMapper.getKeywords(request);

        // "scholarship" appears in both category and mechanism keywords
        // Should only appear once in result
        long scholarshipCount = keywords.stream()
            .filter(k -> k.equalsIgnoreCase("scholarship"))
            .count();

        assertThat(scholarshipCount).isEqualTo(1);
    }

    @Test
    void shouldHandleNullBeneficiariesSet() {
        QueryGenerationRequest request = QueryGenerationRequest.builder()
            .category(FundingSearchCategory.STEM_EDUCATION)
            .beneficiaries(null)
            .build();

        List<String> keywords = categoryMapper.getKeywords(request);

        assertThat(keywords).isNotEmpty();
    }

    @Test
    void shouldHandleEmptyBeneficiariesSet() {
        QueryGenerationRequest request = QueryGenerationRequest.builder()
            .category(FundingSearchCategory.STEM_EDUCATION)
            .beneficiaries(Set.of())
            .build();

        List<String> keywords = categoryMapper.getKeywords(request);

        assertThat(keywords).isNotEmpty();
    }
}
```

**Step 2: Run tests to verify they fail**

```bash
cd northstar-query-generation
mvn test -Dtest=MultiDimensionalCategoryMapperTest
```

Expected: FAIL (compilation error - getKeywords() doesn't accept QueryGenerationRequest yet)

**Step 3: Implement multi-dimensional keyword generation**

Edit `northstar-query-generation/src/main/java/com/northstar/funding/querygeneration/service/CategoryMapper.java`:

Add this new method (keep existing `getKeywords(FundingSearchCategory)` method):

```java
    /**
     * Get keywords from a multi-dimensional query generation request.
     *
     * Combines keywords from all populated dimensions:
     * - category (required)
     * - sourceType (optional)
     * - mechanism (optional)
     * - projectScale (optional)
     * - beneficiaries (optional, can be multiple)
     * - recipientType (optional)
     *
     * Uses Set for automatic deduplication.
     *
     * @param request the query generation request with multiple dimensions
     * @return combined list of unique keywords
     */
    public List<String> getKeywords(QueryGenerationRequest request) {
        Set<String> keywords = new HashSet<>();

        // REQUIRED: Always add category keywords
        keywords.addAll(getKeywords(request.getCategory()));

        // OPTIONAL: Add keywords from populated dimensions
        if (request.getSourceType() != null) {
            keywords.addAll(getSourceTypeKeywords(request.getSourceType()));
        }

        if (request.getMechanism() != null) {
            keywords.addAll(getMechanismKeywords(request.getMechanism()));
        }

        if (request.getProjectScale() != null) {
            keywords.addAll(getProjectScaleKeywords(request.getProjectScale()));
        }

        if (request.getBeneficiaries() != null && !request.getBeneficiaries().isEmpty()) {
            request.getBeneficiaries().forEach(b ->
                keywords.addAll(getBeneficiaryKeywords(b))
            );
        }

        if (request.getRecipientType() != null) {
            keywords.addAll(getRecipientTypeKeywords(request.getRecipientType()));
        }

        // Convert Set to List for LLM prompt construction
        return new ArrayList<>(keywords);
    }

    /**
     * Get keywords for a funding source type.
     */
    private List<String> getSourceTypeKeywords(FundingSourceType sourceType) {
        return switch (sourceType) {
            case GOVERNMENT_NATIONAL -> List.of(
                "government", "national", "ministry", "federal", "state funding"
            );
            case GOVERNMENT_EU -> List.of(
                "European Union", "EU", "Horizon", "Erasmus", "European Commission"
            );
            case GOVERNMENT_REGIONAL -> List.of(
                "government", "regional", "municipal", "local", "county", "city funding"
            );
            case PRIVATE_FOUNDATION -> List.of(
                "foundation", "private", "philanthropic", "charitable", "nonprofit foundation"
            );
            case CORPORATE_FOUNDATION -> List.of(
                "corporate", "company", "business", "CSR", "corporate foundation"
            );
            case BILATERAL_AID -> List.of(
                "bilateral", "USAID", "GIZ", "British Council", "foreign aid", "development assistance"
            );
            case MULTILATERAL_ORG -> List.of(
                "World Bank", "UN", "UNESCO", "UNICEF", "multilateral", "international organization"
            );
            case COMMUNITY_FOUNDATION -> List.of(
                "community", "local foundation", "grassroots", "neighborhood", "community-based"
            );
            case EDUCATION_ASSOCIATION -> List.of(
                "association", "professional", "education network", "teacher association"
            );
            case CROWDFUNDING_PLATFORM -> List.of(
                "crowdfunding", "Kickstarter", "Indiegogo", "crowdsourced", "community-funded"
            );
            case RELIGIOUS_FOUNDATION -> List.of(
                "religious", "faith-based", "church", "religious organization", "ministry"
            );
            case CREDIT_UNION_OR_BANK -> List.of(
                "bank", "credit union", "financial institution", "education loan", "student loan"
            );
        };
    }

    /**
     * Get keywords for a funding mechanism.
     */
    private List<String> getMechanismKeywords(FundingMechanism mechanism) {
        return switch (mechanism) {
            case GRANT -> List.of(
                "grant", "funding", "award", "non-repayable"
            );
            case LOAN -> List.of(
                "loan", "financing", "credit", "repayable", "borrowing"
            );
            case SCHOLARSHIP -> List.of(
                "scholarship", "bursary", "student award", "tuition support"
            );
            case FELLOWSHIP -> List.of(
                "fellowship", "research fellowship", "professional fellowship", "study fellowship"
            );
            case MATCHING_GRANT -> List.of(
                "matching grant", "matching funds", "co-funding", "matched funding"
            );
            case PRIZE_AWARD -> List.of(
                "prize", "award", "competition", "recognition", "achievement award"
            );
            case IN_KIND_DONATION -> List.of(
                "in-kind", "donation", "equipment", "materials", "non-monetary"
            );
            case SUBSIDY -> List.of(
                "subsidy", "tax benefit", "government support", "financial assistance"
            );
        };
    }

    /**
     * Get keywords for a project scale.
     */
    private List<String> getProjectScaleKeywords(ProjectScale scale) {
        return switch (scale) {
            case MICRO -> List.of(
                "micro grant", "small amount", "under 5k", "mini grant"
            );
            case SMALL -> List.of(
                "small grant", "5k-50k", "moderate funding", "small scale"
            );
            case MEDIUM -> List.of(
                "medium grant", "50k-250k", "moderate scale", "mid-size funding"
            );
            case LARGE -> List.of(
                "large grant", "250k-1M", "substantial funding", "major grant"
            );
            case MEGA -> List.of(
                "mega grant", "over 1M", "major funding", "large scale", "significant investment"
            );
        };
    }

    /**
     * Get keywords for a beneficiary population.
     */
    private List<String> getBeneficiaryKeywords(BeneficiaryPopulation population) {
        return switch (population) {
            case LOW_INCOME_FAMILIES -> List.of(
                "low income", "economically disadvantaged", "poverty", "financial need"
            );
            case RURAL_COMMUNITIES -> List.of(
                "rural", "remote", "countryside", "village", "underserved areas"
            );
            case ETHNIC_MINORITIES -> List.of(
                "minority", "ethnic", "underrepresented", "diverse communities"
            );
            case GIRLS_WOMEN -> List.of(
                "girls", "women", "female", "gender equality", "women's education"
            );
            case CHILDREN_AGES_4_12 -> List.of(
                "children", "primary school age", "ages 4-12", "elementary", "young learners"
            );
            case ADOLESCENTS_AGES_13_18 -> List.of(
                "adolescents", "teenagers", "secondary school", "ages 13-18", "youth"
            );
            case FIRST_GENERATION_STUDENTS -> List.of(
                "first generation", "family access", "first to college", "pioneering students"
            );
            case AT_RISK_YOUTH -> List.of(
                "at-risk", "vulnerable", "disadvantaged youth", "youth support"
            );
            case PEOPLE_WITH_DISABILITIES -> List.of(
                "disabilities", "special needs", "accessibility", "inclusive education"
            );
            case REFUGEES_IMMIGRANTS -> List.of(
                "refugee", "immigrant", "displaced", "newcomer", "migrant"
            );
            case LANGUAGE_MINORITIES -> List.of(
                "language minority", "non-native speakers", "bilingual", "multilingual"
            );
            case EARLY_CHILDHOOD_0_5 -> List.of(
                "early childhood", "preschool", "ages 0-5", "infant", "toddler"
            );
            case ADULTS_LIFELONG_LEARNING -> List.of(
                "adult learners", "lifelong learning", "continuing education", "mature students"
            );
            case EDUCATORS_TEACHERS -> List.of(
                "teachers", "educators", "instructors", "teaching professionals"
            );
            case GENERAL_POPULATION -> List.of(
                "general population", "all ages", "broad access", "universal"
            );
            case LGBTQ_PLUS -> List.of(
                "LGBTQ", "gender identity", "sexual orientation", "LGBTQ+ community"
            );
            case VETERANS -> List.of(
                "veterans", "military", "ex-service", "veteran families"
            );
            case ELDERLY -> List.of(
                "elderly", "seniors", "older adults", "senior citizens"
            );
        };
    }

    /**
     * Get keywords for a recipient organization type.
     */
    private List<String> getRecipientTypeKeywords(RecipientOrganizationType type) {
        return switch (type) {
            case PRIVATE_LANGUAGE_SCHOOL -> List.of(
                "language school", "private education", "language center", "training center"
            );
            case K12_PRIVATE_SCHOOL -> List.of(
                "private school", "K-12", "independent school", "non-public school"
            );
            case K12_PUBLIC_SCHOOL -> List.of(
                "public school", "K-12", "state school", "government school"
            );
            case PRESCHOOL_EARLY_CHILDHOOD -> List.of(
                "preschool", "kindergarten", "early childhood center", "nursery"
            );
            case EXAMINATION_CENTER -> List.of(
                "examination center", "testing center", "assessment center", "exam preparation"
            );
            case NGO_EDUCATION_FOCUSED -> List.of(
                "NGO", "nonprofit", "education NGO", "educational nonprofit"
            );
            case NGO_SOCIAL_SERVICES -> List.of(
                "social services", "community organization", "social NGO", "welfare organization"
            );
            case FOR_PROFIT_EDUCATION -> List.of(
                "for-profit", "commercial", "private company", "education business"
            );
            case UNIVERSITY_PUBLIC -> List.of(
                "university", "college", "higher education", "post-secondary"
            );
            case MUNICIPALITY -> List.of(
                "municipality", "local government", "city", "town", "county"
            );
            case INDIVIDUAL_EDUCATOR -> List.of(
                "teacher", "educator", "instructor", "individual professional"
            );
            case INDIVIDUAL_STUDENT -> List.of(
                "student", "learner", "individual", "pupil"
            );
            case RESEARCH_INSTITUTE -> List.of(
                "research institute", "think tank", "research center", "academic research"
            );
            case LIBRARY_OR_CULTURAL_CENTER -> List.of(
                "library", "museum", "cultural center", "arts center", "community center"
            );
        };
    }
```

Add imports at the top:

```java
import com.northstar.funding.domain.*;
import com.northstar.funding.querygeneration.dto.QueryGenerationRequest;
import java.util.HashSet;
import java.util.Set;
```

**Step 4: Run tests to verify they pass**

```bash
cd northstar-query-generation
mvn test -Dtest=MultiDimensionalCategoryMapperTest
```

Expected: PASS (7 tests, 0 failures)

**Step 5: Run all query-generation tests**

```bash
cd northstar-query-generation
mvn test
```

Expected: PASS (all tests including new and existing)

**Step 6: Commit**

```bash
git add northstar-query-generation/src/main/java/com/northstar/funding/querygeneration/service/CategoryMapper.java
git add northstar-query-generation/src/test/java/com/northstar/funding/querygeneration/service/MultiDimensionalCategoryMapperTest.java
git commit -m "feat: Implement multi-dimensional keyword generation in CategoryMapper"
```

---

## Task 10: Create Scheduler Module (Maven Module Setup)

**Files:**
- Create: `northstar-scheduler/pom.xml`
- Modify: `pom.xml` (root)

**Step 1: Create scheduler module directory structure**

```bash
mkdir -p northstar-scheduler/src/main/java/com/northstar/funding/scheduler
mkdir -p northstar-scheduler/src/main/resources
mkdir -p northstar-scheduler/src/test/java/com/northstar/funding/scheduler
mkdir -p northstar-scheduler/src/test/resources
```

**Step 2: Create scheduler module POM**

Create `northstar-scheduler/pom.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.northstar.funding</groupId>
        <artifactId>northstar-funding</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>

    <artifactId>northstar-scheduler</artifactId>
    <name>NorthStar Scheduler</name>
    <description>Scheduling and batch orchestration for NorthStar Funding Discovery</description>

    <dependencies>
        <!-- Domain module for enums -->
        <dependency>
            <groupId>com.northstar.funding</groupId>
            <artifactId>northstar-domain</artifactId>
            <version>${project.version}</version>
        </dependency>

        <!-- Query generation module for request DTOs -->
        <dependency>
            <groupId>com.northstar.funding</groupId>
            <artifactId>northstar-query-generation</artifactId>
            <version>${project.version}</version>
        </dependency>

        <!-- Spring Boot -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter</artifactId>
        </dependency>

        <!-- Vavr for functional programming -->
        <dependency>
            <groupId>io.vavr</groupId>
            <artifactId>vavr</artifactId>
        </dependency>

        <!-- Testing -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
            </plugin>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-surefire-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

**Step 3: Add scheduler module to root POM**

Edit `pom.xml` (root):

Find the `<modules>` section and add:

```xml
        <module>northstar-scheduler</module>
```

**Step 4: Verify module structure compiles**

```bash
mvn clean compile
```

Expected: SUCCESS (all modules including new scheduler module)

**Step 5: Commit**

```bash
git add northstar-scheduler/pom.xml
git add pom.xml
git commit -m "feat: Create northstar-scheduler Maven module"
```

---

## Task 11: Implement DailyScheduleService

**Files:**
- Create: `northstar-scheduler/src/main/java/com/northstar/funding/scheduler/service/DailyScheduleService.java`
- Create: `northstar-scheduler/src/test/java/com/northstar/funding/scheduler/service/DailyScheduleServiceTest.java`

**Step 1: Write failing tests**

Create `northstar-scheduler/src/test/java/com/northstar/funding/scheduler/service/DailyScheduleServiceTest.java`:

```java
package com.northstar.funding.scheduler.service;

import com.northstar.funding.domain.*;
import com.northstar.funding.querygeneration.dto.QueryGenerationRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DailyScheduleServiceTest {

    private DailyScheduleService scheduleService;

    @BeforeEach
    void setUp() {
        scheduleService = new DailyScheduleService(20);
    }

    @Test
    void shouldGenerateBatchForMonday() {
        List<QueryGenerationRequest> batch = scheduleService.generateBatchForDay(DayOfWeek.MONDAY);

        assertThat(batch)
            .isNotEmpty()
            .hasSizeLessThanOrEqualTo(20);

        // Monday: Government sources + STEM categories
        assertThat(batch)
            .anySatisfy(req -> {
                assertThat(req.getSourceType()).isIn(
                    FundingSourceType.GOVERNMENT_NATIONAL,
                    FundingSourceType.GOVERNMENT_EU,
                    FundingSourceType.GOVERNMENT_REGIONAL
                );
                assertThat(req.getCategory()).isIn(
                    FundingSearchCategory.STEM_EDUCATION,
                    FundingSearchCategory.DIGITAL_LITERACY,
                    FundingSearchCategory.ENVIRONMENTAL_EDUCATION
                );
            });
    }

    @Test
    void shouldGenerateBatchForTuesday() {
        List<QueryGenerationRequest> batch = scheduleService.generateBatchForDay(DayOfWeek.TUESDAY);

        assertThat(batch)
            .isNotEmpty()
            .hasSizeLessThanOrEqualTo(20);

        // Tuesday: Foundations + Language categories
        assertThat(batch)
            .anySatisfy(req -> {
                assertThat(req.getSourceType()).isIn(
                    FundingSourceType.PRIVATE_FOUNDATION,
                    FundingSourceType.COMMUNITY_FOUNDATION
                );
                assertThat(req.getCategory()).isIn(
                    FundingSearchCategory.LANGUAGE_EDUCATION,
                    FundingSearchCategory.FOREIGN_LANGUAGES,
                    FundingSearchCategory.MULTICULTURAL_EDUCATION
                );
            });
    }

    @Test
    void shouldGenerateBatchForWednesday() {
        List<QueryGenerationRequest> batch = scheduleService.generateBatchForDay(DayOfWeek.WEDNESDAY);

        assertThat(batch)
            .isNotEmpty()
            .hasSizeLessThanOrEqualTo(20);

        // Wednesday: Multilateral/Bilateral + Scholarships
        assertThat(batch)
            .anySatisfy(req -> {
                assertThat(req.getSourceType()).isIn(
                    FundingSourceType.MULTILATERAL_ORG,
                    FundingSourceType.BILATERAL_AID
                );
                assertThat(req.getCategory()).isIn(
                    FundingSearchCategory.SCHOLARSHIPS_FINANCIAL_AID,
                    FundingSearchCategory.TEACHER_DEVELOPMENT
                );
            });
    }

    @Test
    void shouldRespectQueriesPerNightLimit() {
        DailyScheduleService limitedService = new DailyScheduleService(10);

        List<QueryGenerationRequest> batch = limitedService.generateBatchForDay(DayOfWeek.MONDAY);

        assertThat(batch).hasSizeLessThanOrEqualTo(10);
    }

    @Test
    void shouldGenerateBatchForCurrentDay() {
        List<QueryGenerationRequest> batch = scheduleService.generateDailyBatch();

        assertThat(batch)
            .isNotEmpty()
            .hasSizeLessThanOrEqualTo(20);
    }

    @Test
    void allRequestsShouldHaveRequiredFields() {
        List<QueryGenerationRequest> batch = scheduleService.generateBatchForDay(DayOfWeek.MONDAY);

        assertThat(batch).allSatisfy(req -> {
            assertThat(req.getSearchEngine()).isNotNull();
            assertThat(req.getCategory()).isNotNull();
            assertThat(req.getGeographicScope()).isNotNull();
        });
    }

    @Test
    void shouldRotateSearchEngines() {
        List<QueryGenerationRequest> batch = scheduleService.generateBatchForDay(DayOfWeek.MONDAY);

        long distinctEngines = batch.stream()
            .map(QueryGenerationRequest::getSearchEngine)
            .distinct()
            .count();

        // Should use multiple search engines
        assertThat(distinctEngines).isGreaterThan(1);
    }
}
```

**Step 2: Run tests to verify they fail**

```bash
cd northstar-scheduler
mvn test -Dtest=DailyScheduleServiceTest
```

Expected: FAIL (class does not exist)

**Step 3: Write minimal implementation**

Create `northstar-scheduler/src/main/java/com/northstar/funding/scheduler/service/DailyScheduleService.java`:

```java
package com.northstar.funding.scheduler.service;

import com.northstar.funding.domain.*;
import com.northstar.funding.querygeneration.dto.QueryGenerationRequest;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Generates daily batches of query generation requests based on a fixed weekly schedule.
 *
 * Each day focuses on specific source types and categories to ensure comprehensive
 * coverage of funding opportunities over the week.
 */
@Service
public class DailyScheduleService {

    private final int queriesPerNight;

    /**
     * Create service with configurable query limit.
     *
     * @param queriesPerNight maximum queries to generate per night (typically 20-25)
     */
    public DailyScheduleService(int queriesPerNight) {
        this.queriesPerNight = queriesPerNight;
    }

    /**
     * Generate batch of queries for today's schedule.
     *
     * @return list of query generation requests (up to queriesPerNight)
     */
    public List<QueryGenerationRequest> generateDailyBatch() {
        DayOfWeek today = LocalDate.now().getDayOfWeek();
        return generateBatchForDay(today);
    }

    /**
     * Generate batch of queries for specific day.
     *
     * @param day the day of week
     * @return list of query generation requests (up to queriesPerNight)
     */
    public List<QueryGenerationRequest> generateBatchForDay(DayOfWeek day) {
        List<QueryGenerationRequest> batch = new ArrayList<>();

        switch (day) {
            case MONDAY -> batch.addAll(generateGovernmentSTEMQueries());
            case TUESDAY -> batch.addAll(generateFoundationsLanguagesQueries());
            case WEDNESDAY -> batch.addAll(generateScholarshipsMultilateralQueries());
            case THURSDAY -> batch.addAll(generateInfrastructureBilateralQueries());
            case FRIDAY -> batch.addAll(generateVocationalCorporateQueries());
            case SATURDAY -> batch.addAll(generateEarlyChildhoodCommunityQueries());
            case SUNDAY -> batch.addAll(generateArtsResearchQueries());
        }

        // Limit to configured number of queries per night
        return batch.subList(0, Math.min(batch.size(), queriesPerNight));
    }

    /**
     * Monday: Government sources + STEM categories.
     */
    private List<QueryGenerationRequest> generateGovernmentSTEMQueries() {
        List<QueryGenerationRequest> queries = new ArrayList<>();

        FundingSourceType[] govSources = {
            FundingSourceType.GOVERNMENT_NATIONAL,
            FundingSourceType.GOVERNMENT_EU,
            FundingSourceType.GOVERNMENT_REGIONAL
        };

        FundingSearchCategory[] stemCategories = {
            FundingSearchCategory.STEM_EDUCATION,
            FundingSearchCategory.DIGITAL_LITERACY,
            FundingSearchCategory.ENVIRONMENTAL_EDUCATION
        };

        SearchEngineType[] engines = {SearchEngineType.TAVILY, SearchEngineType.SEARXNG};
        GeographicScope[] scopes = {GeographicScope.BULGARIA, GeographicScope.EASTERN_EUROPE, GeographicScope.EU};

        for (FundingSourceType source : govSources) {
            for (FundingSearchCategory category : stemCategories) {
                for (GeographicScope scope : scopes) {
                    // Rotate through search engines
                    SearchEngineType engine = engines[queries.size() % engines.length];

                    queries.add(QueryGenerationRequest.builder()
                        .searchEngine(engine)
                        .category(category)
                        .geographicScope(scope)
                        .sourceType(source)
                        .numberOfQueries(3)
                        .build());
                }
            }
        }

        return queries;
    }

    /**
     * Tuesday: Foundations + Language categories.
     */
    private List<QueryGenerationRequest> generateFoundationsLanguagesQueries() {
        List<QueryGenerationRequest> queries = new ArrayList<>();

        FundingSourceType[] foundationSources = {
            FundingSourceType.PRIVATE_FOUNDATION,
            FundingSourceType.COMMUNITY_FOUNDATION
        };

        FundingSearchCategory[] languageCategories = {
            FundingSearchCategory.LANGUAGE_EDUCATION,
            FundingSearchCategory.FOREIGN_LANGUAGES,
            FundingSearchCategory.MULTICULTURAL_EDUCATION
        };

        SearchEngineType[] engines = {SearchEngineType.TAVILY, SearchEngineType.SEARXNG};
        GeographicScope[] scopes = {GeographicScope.BULGARIA, GeographicScope.BALKANS, GeographicScope.EU};

        for (FundingSourceType source : foundationSources) {
            for (FundingSearchCategory category : languageCategories) {
                for (GeographicScope scope : scopes) {
                    SearchEngineType engine = engines[queries.size() % engines.length];

                    queries.add(QueryGenerationRequest.builder()
                        .searchEngine(engine)
                        .category(category)
                        .geographicScope(scope)
                        .sourceType(source)
                        .numberOfQueries(3)
                        .build());
                }
            }
        }

        return queries;
    }

    /**
     * Wednesday: Multilateral/Bilateral + Scholarships.
     */
    private List<QueryGenerationRequest> generateScholarshipsMultilateralQueries() {
        List<QueryGenerationRequest> queries = new ArrayList<>();

        FundingSourceType[] intlSources = {
            FundingSourceType.MULTILATERAL_ORG,
            FundingSourceType.BILATERAL_AID
        };

        FundingSearchCategory[] scholarshipCategories = {
            FundingSearchCategory.SCHOLARSHIPS_FINANCIAL_AID,
            FundingSearchCategory.TEACHER_DEVELOPMENT
        };

        SearchEngineType[] engines = {SearchEngineType.TAVILY, SearchEngineType.SEARXNG};
        GeographicScope[] scopes = {GeographicScope.EASTERN_EUROPE, GeographicScope.EU, GeographicScope.INTERNATIONAL};

        for (FundingSourceType source : intlSources) {
            for (FundingSearchCategory category : scholarshipCategories) {
                for (GeographicScope scope : scopes) {
                    SearchEngineType engine = engines[queries.size() % engines.length];

                    queries.add(QueryGenerationRequest.builder()
                        .searchEngine(engine)
                        .category(category)
                        .geographicScope(scope)
                        .sourceType(source)
                        .mechanism(FundingMechanism.SCHOLARSHIP)
                        .numberOfQueries(3)
                        .build());
                }
            }
        }

        return queries;
    }

    /**
     * Thursday: Government/Bilateral + Infrastructure.
     */
    private List<QueryGenerationRequest> generateInfrastructureBilateralQueries() {
        List<QueryGenerationRequest> queries = new ArrayList<>();

        FundingSourceType[] sources = {
            FundingSourceType.GOVERNMENT_REGIONAL,
            FundingSourceType.BILATERAL_AID
        };

        FundingSearchCategory[] infraCategories = {
            FundingSearchCategory.INFRASTRUCTURE_FACILITIES,
            FundingSearchCategory.ACCESSIBILITY_INCLUSION
        };

        SearchEngineType[] engines = {SearchEngineType.TAVILY, SearchEngineType.SEARXNG};
        GeographicScope[] scopes = {GeographicScope.BULGARIA, GeographicScope.BALKANS};

        for (FundingSourceType source : sources) {
            for (FundingSearchCategory category : infraCategories) {
                for (GeographicScope scope : scopes) {
                    for (ProjectScale scale : new ProjectScale[]{ProjectScale.MEDIUM, ProjectScale.LARGE}) {
                        SearchEngineType engine = engines[queries.size() % engines.length];

                        queries.add(QueryGenerationRequest.builder()
                            .searchEngine(engine)
                            .category(category)
                            .geographicScope(scope)
                            .sourceType(source)
                            .projectScale(scale)
                            .numberOfQueries(3)
                            .build());
                    }
                }
            }
        }

        return queries;
    }

    /**
     * Friday: Corporate + Vocational.
     */
    private List<QueryGenerationRequest> generateVocationalCorporateQueries() {
        List<QueryGenerationRequest> queries = new ArrayList<>();

        FundingSourceType[] corporateSources = {
            FundingSourceType.CORPORATE_FOUNDATION,
            FundingSourceType.EDUCATION_ASSOCIATION
        };

        FundingSearchCategory[] vocationalCategories = {
            FundingSearchCategory.VOCATIONAL_TRAINING,
            FundingSearchCategory.ENTREPRENEURSHIP_EDUCATION,
            FundingSearchCategory.CAREER_EDUCATION
        };

        SearchEngineType[] engines = {SearchEngineType.TAVILY, SearchEngineType.SEARXNG};
        GeographicScope[] scopes = {GeographicScope.BULGARIA, GeographicScope.EASTERN_EUROPE};

        for (FundingSourceType source : corporateSources) {
            for (FundingSearchCategory category : vocationalCategories) {
                for (GeographicScope scope : scopes) {
                    SearchEngineType engine = engines[queries.size() % engines.length];

                    queries.add(QueryGenerationRequest.builder()
                        .searchEngine(engine)
                        .category(category)
                        .geographicScope(scope)
                        .sourceType(source)
                        .numberOfQueries(3)
                        .build());
                }
            }
        }

        return queries;
    }

    /**
     * Saturday: Community foundations + Early childhood.
     */
    private List<QueryGenerationRequest> generateEarlyChildhoodCommunityQueries() {
        List<QueryGenerationRequest> queries = new ArrayList<>();

        FundingSourceType[] communitySources = {
            FundingSourceType.COMMUNITY_FOUNDATION,
            FundingSourceType.RELIGIOUS_FOUNDATION
        };

        FundingSearchCategory[] earlyChildhoodCategories = {
            FundingSearchCategory.EARLY_CHILDHOOD_EDUCATION,
            FundingSearchCategory.PARENT_ENGAGEMENT
        };

        SearchEngineType[] engines = {SearchEngineType.TAVILY, SearchEngineType.SEARXNG};
        GeographicScope[] scopes = {GeographicScope.BULGARIA, GeographicScope.BALKANS};

        for (FundingSourceType source : communitySources) {
            for (FundingSearchCategory category : earlyChildhoodCategories) {
                for (GeographicScope scope : scopes) {
                    SearchEngineType engine = engines[queries.size() % engines.length];

                    queries.add(QueryGenerationRequest.builder()
                        .searchEngine(engine)
                        .category(category)
                        .geographicScope(scope)
                        .sourceType(source)
                        .projectScale(ProjectScale.SMALL)
                        .numberOfQueries(3)
                        .build());
                }
            }
        }

        return queries;
    }

    /**
     * Sunday: Various sources + Arts/Research.
     */
    private List<QueryGenerationRequest> generateArtsResearchQueries() {
        List<QueryGenerationRequest> queries = new ArrayList<>();

        FundingSourceType[] sources = {
            FundingSourceType.PRIVATE_FOUNDATION,
            FundingSourceType.CROWDFUNDING_PLATFORM
        };

        FundingSearchCategory[] categories = {
            FundingSearchCategory.ARTS_CULTURE,
            FundingSearchCategory.EDUCATIONAL_RESEARCH
        };

        SearchEngineType[] engines = {SearchEngineType.TAVILY, SearchEngineType.SEARXNG};
        GeographicScope[] scopes = {GeographicScope.BULGARIA, GeographicScope.EU};

        for (FundingSourceType source : sources) {
            for (FundingSearchCategory category : categories) {
                for (GeographicScope scope : scopes) {
                    SearchEngineType engine = engines[queries.size() % engines.length];

                    queries.add(QueryGenerationRequest.builder()
                        .searchEngine(engine)
                        .category(category)
                        .geographicScope(scope)
                        .sourceType(source)
                        .numberOfQueries(3)
                        .build());
                }
            }
        }

        return queries;
    }
}
```

**Step 4: Run tests to verify they pass**

```bash
cd northstar-scheduler
mvn test -Dtest=DailyScheduleServiceTest
```

Expected: PASS (8 tests, 0 failures)

**Step 5: Run all scheduler tests**

```bash
cd northstar-scheduler
mvn test
```

Expected: PASS

**Step 6: Commit**

```bash
git add northstar-scheduler/src/main/java/com/northstar/funding/scheduler/service/DailyScheduleService.java
git add northstar-scheduler/src/test/java/com/northstar/funding/scheduler/service/DailyScheduleServiceTest.java
git commit -m "feat: Implement DailyScheduleService with fixed weekly schedule"
```

---

## Task 12: Run Full Test Suite and Verify Backward Compatibility

**Step 1: Run all tests across all modules**

```bash
mvn clean test
```

Expected: All 421+ tests PASS (including new tests)

**Step 2: Verify Feature 004 tests still pass**

```bash
cd northstar-query-generation
mvn test -Dtest='*IntegrationTest'
```

Expected: All Feature 004 integration tests PASS

**Step 3: Check test count**

```bash
mvn test 2>&1 | grep "Tests run:"
```

Expected: Tests run: 450+, Failures: 0, Errors: 0

**Step 4: Document test results**

If all tests pass, proceed to final commit. If any tests fail, debug and fix before proceeding.

---

## Task 13: Final Integration and Documentation

**Step 1: Update CLAUDE.md with Feature 005 information**

Edit `CLAUDE.md`:

Add section under "## What's Next?":

```markdown
### Feature 005: Enhanced Taxonomy & Basic Scheduler (COMPLETE)

**Status**: ✅ COMPLETE

**Added**:
- 6 new domain enums (66 total values)
- Multi-dimensional query support with 7 optional dimensions
- Enhanced CategoryMapper for multi-dimensional keyword generation
- Basic scheduler with fixed daily schedule (20-25 queries per night)
- New northstar-scheduler Maven module

**Test Coverage**: 450+ tests passing

**Usage**:
```java
// Multi-dimensional query example
QueryGenerationRequest request = QueryGenerationRequest.builder()
    .searchEngine(SearchEngineType.TAVILY)
    .category(FundingSearchCategory.INFRASTRUCTURE_FACILITIES)
    .geographicScope(GeographicScope.BULGARIA)
    .sourceType(FundingSourceType.GOVERNMENT_REGIONAL)
    .mechanism(FundingMechanism.LOAN)
    .projectScale(ProjectScale.MEDIUM)
    .recipientType(RecipientOrganizationType.PRIVATE_LANGUAGE_SCHOOL)
    .numberOfQueries(3)
    .build();

// Scheduler usage
DailyScheduleService scheduler = new DailyScheduleService(20);
List<QueryGenerationRequest> batch = scheduler.generateDailyBatch();
```

**Backward Compatibility**: ✅ Feature 004 code unchanged
```

**Step 2: Commit documentation update**

```bash
git add CLAUDE.md
git commit -m "docs: Update CLAUDE.md with Feature 005 completion status"
```

**Step 3: Create session summary**

Update `northstar-notes/session-summaries/2025-11-04-feature-005-brainstorming-in-progress.md`:

Change filename to: `2025-11-04-feature-005-implementation-complete.md`

Update status to: **✅ COMPLETE**

**Step 4: Commit session summary**

```bash
git add northstar-notes/session-summaries/
git commit -m "docs: Mark Feature 005 as complete in session summary"
```

---

## Success Criteria

✅ **All Tasks Complete**: 13/13 tasks finished
✅ **Test Coverage**: 450+ tests passing (0 failures, 0 errors)
✅ **Backward Compatibility**: Feature 004 tests unchanged and passing
✅ **Enum Values**: 66 new enum values across 6 enums
✅ **Multi-Dimensional Queries**: CategoryMapper handles 0-7 optional dimensions
✅ **Scheduler**: Fixed weekly schedule generating 20-25 queries per day
✅ **Documentation**: CLAUDE.md and session summaries updated

---

## Next Steps (Post-Implementation)

1. **Manual Testing**: Generate sample queries with multi-dimensional requests
2. **Query Quality Review**: Manually review generated queries for precision
3. **Scheduler Testing**: Run daily batches for each day of week
4. **Merge to Main**: Create PR with comprehensive description
5. **Feature 006 Planning**: Phase 2 deep crawling (robots.txt, sitemaps)

---

**Plan Status**: ✅ COMPLETE - Ready for Execution
**Estimated Time**: 2-3 development sessions (4-6 hours)
**Risk Level**: Low (backward compatible, validate-through-implementation)
**Implementation Mode**: TDD with frequent commits
