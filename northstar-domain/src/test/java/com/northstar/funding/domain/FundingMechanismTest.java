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
