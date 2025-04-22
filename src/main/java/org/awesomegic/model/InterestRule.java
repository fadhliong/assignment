package org.awesomegic.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public record InterestRule(LocalDate effectiveDate, String ruleId, BigDecimal interestRate) {
    public InterestRule {
        if (effectiveDate == null) {
            throw new IllegalArgumentException("Effective date cannot be null");
        }
        if (ruleId == null || ruleId.isBlank()) {
            throw new IllegalArgumentException("Rule ID cannot be null or empty");
        }
        if (interestRate == null ||
                interestRate.compareTo(BigDecimal.ZERO) <= 0 ||
                interestRate.compareTo(BigDecimal.valueOf(100)) >= 0) {
            throw new IllegalArgumentException("Interest rate must be between 0 and 100");
        }
    }
}
