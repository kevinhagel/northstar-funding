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
