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
