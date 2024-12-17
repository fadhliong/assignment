package org.crypto.assignment.model.enums;

public enum TradeType {
    BUY("Buy order at market price"),
    SELL("Sell order at market price");

    private final String description;

    TradeType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
