package org.crypto.assignment.model.enums;

public enum WalletStatus {
    ACTIVE("Wallet is active"),
    SUSPENDED("Wallet is suspended"),
    CLOSED("Wallet is permanently closed");

    private String description;

    WalletStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
