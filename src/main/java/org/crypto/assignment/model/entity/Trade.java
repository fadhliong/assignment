package org.crypto.assignment.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.crypto.assignment.model.enums.TradeStatus;
import org.crypto.assignment.model.enums.TradeType;
import org.crypto.assignment.model.enums.TradingPair;
import org.springframework.data.annotation.CreatedDate;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "trade", indexes = {
        @Index(name = "idx_trade_wallet", columnList = "wallet_id"),
        @Index(name = "idx_trade_status", columnList = "status"),
        @Index(name = "idx_trade_created_at", columnList = "created_at"),
        @Index(name = "idx_trades_idempotency_key", columnList = "idempotency_key", unique = true)
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Trade {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id", nullable = false)
    private Wallet wallet;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TradingPair tradingPair;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TradeType tradeType;

    @Column(nullable = false, precision = 20, scale = 8)
    private BigDecimal amount;

    @Column(nullable = false, precision = 20, scale = 8)
    private BigDecimal price;

    @Column(precision = 20, scale = 8)
    private BigDecimal executionPrice;

    @Column(nullable = false, precision = 20, scale = 8)
    private BigDecimal marketPrice;

    @Column(nullable = false, precision = 20, scale = 8)
    private BigDecimal totalMarketValue;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TradeStatus status;

    @Version
    private Long version;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "idempotency_key", nullable = false, length = 50)
    private String idempotencyKey;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Column
    private LocalDateTime executedAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
