package org.awesomegic.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public record Account(String accountNumber, BigDecimal balance, LocalDate createdDate) {
    public Account {
        if (accountNumber == null || accountNumber.isBlank()) {
            throw new IllegalArgumentException("Account number cannot be null or empty");
        }
        if (balance == null) {
            throw new IllegalArgumentException("Balance cannot be null");
        }
        if (balance.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Initial balance cannot be negative");
        }
        if (createdDate == null) {
            throw new IllegalArgumentException("Creation date cannot be null");
        }
    }

    public static Account createNew(String accountNumber) {
        return new Account(accountNumber, BigDecimal.ZERO, LocalDate.now());
    }
}
