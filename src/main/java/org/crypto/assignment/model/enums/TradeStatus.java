package org.crypto.assignment.model.enums;

public enum TradeStatus {
    PENDING("Trade initiated but not yet processed"),
    EXECUTED("Trade successfully executed"),
    COMPLETED("Trade successfully completed"),
    FAILED("Trade failed due to an error"),
    CANCELLED("Trade cancelled by user");

    private final String description;

    TradeStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
