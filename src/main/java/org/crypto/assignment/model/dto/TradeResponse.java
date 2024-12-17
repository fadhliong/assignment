package org.crypto.assignment.model.dto;

import lombok.Builder;
import lombok.Value;
import org.crypto.assignment.model.entity.Trade;
import org.crypto.assignment.model.enums.TradeStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Value
@Builder
public class TradeResponse {
    Long tradeId;
    Long walletId;
    String tradingPair;
    String tradeType;
    BigDecimal amount;
    BigDecimal requestedPrice;
    BigDecimal executionPrice;
    BigDecimal totalValue;
    TradeStatus status;
    LocalDateTime executedAt;

    public static TradeResponse fromTrade(Trade trade) {
        return TradeResponse.builder()
                .tradeId(trade.getId())
                .walletId(trade.getWallet().getWalletId())
                .tradingPair(trade.getTradingPair().name())
                .tradeType(trade.getTradeType().name())
                .amount(trade.getAmount())
                .requestedPrice(trade.getPrice())
                .executionPrice(trade.getExecutionPrice())
                .totalValue(trade.getTotalMarketValue())
                .status(trade.getStatus())
                .executedAt(trade.getCreatedAt())
                .build();
    }
}
