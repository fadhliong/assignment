package org.crypto.assignment.model.enums;

import java.math.BigDecimal;

public enum TradingPair {
    BTCUSDT("Bitcoin/USDT"),
    ETHUSDT("Ethereum/USDT");

    private final String description;

    TradingPair(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

}
