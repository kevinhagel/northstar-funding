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
