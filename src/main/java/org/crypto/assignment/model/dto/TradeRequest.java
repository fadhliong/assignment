package org.crypto.assignment.model.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Builder;
import lombok.Value;
import org.crypto.assignment.model.enums.TradeType;
import org.crypto.assignment.model.enums.TradingPair;

import java.math.BigDecimal;

@Value
@Builder
public class TradeRequest {
    @NotNull(message = "Idempotency key is required")
    @Pattern(regexp = "^[a-zA-Z0-9-_]{1,50}$", message = "Invalid idempotency key format")
    String idempotencyKey;

    @NotNull(message = "User ID is required")
    Long userId;

    @NotNull(message = "Wallet ID is required")
    Long walletId;

    @NotNull(message = "Trading pair is required")
    TradingPair tradingPair;

    @NotNull(message = "Trade type is required")
    TradeType tradeType;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Amount must be greater than 0")
    BigDecimal amount;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
    BigDecimal price;
}
