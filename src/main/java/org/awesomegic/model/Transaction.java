package org.awesomegic.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

public record Transaction(String id, LocalDate date, String accountNumber,
                          TransactionType type, BigDecimal amount, BigDecimal balance) {

    public enum TransactionType {
        DEPOSIT("D"),
        WITHDRAWAL("W"),
        INTEREST("I");

        private final String code;

        TransactionType(String code) {
            this.code = code;
        }

        public String getCode() {
            return code;
        }

        public static TransactionType fromCode(String code) {
            if (code == null) {
                throw new IllegalArgumentException("Transaction type code cannot be null");
            }

            return switch (code.toUpperCase()) {
                case "D" -> DEPOSIT;
                case "W" -> WITHDRAWAL;
                case "I" -> INTEREST;
                default -> throw new IllegalArgumentException("Unknown transaction type code: " + code);
            };
        }

        @Override
        public String toString() {
            return code;
        }
    }

    public Transaction {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("Id cannot be null or empty");
        }
        if(date == null) {
            throw new IllegalArgumentException("Transaction date cannot be null");
        }
        if(accountNumber==null || accountNumber.isBlank()) {
            throw new IllegalArgumentException("Account number cannot be null or empty");
        }
        if(type == null) {
            throw new IllegalArgumentException("Transaction type cannot be null");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        amount = amount.setScale(2, RoundingMode.HALF_UP);
    }
}
