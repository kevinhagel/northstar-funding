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
