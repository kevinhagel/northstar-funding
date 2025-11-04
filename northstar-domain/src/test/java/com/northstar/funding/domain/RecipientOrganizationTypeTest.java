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
