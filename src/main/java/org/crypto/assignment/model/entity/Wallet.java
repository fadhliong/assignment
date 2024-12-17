package org.crypto.assignment.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.crypto.assignment.model.enums.TradeType;
import org.crypto.assignment.model.enums.TradingPair;
import org.crypto.assignment.model.enums.WalletStatus;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "wallet", indexes = {
        @Index(name = "idx_wallet_user", columnList = "user_id"),
        @Index(name = "idx_wallet_id", columnList = "wallet_id"),
        @Index(name = "idx_wallet_status", columnList = "status"),
        @Index(name = "idx_wallet_user_wallet", columnList = "user_id, wallet_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Wallet {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "wallet_id", nullable = false)
    private Long walletId;

    @Column(nullable = false, precision = 20, scale = 8)
    private BigDecimal usdtBalance;

    @Column(nullable = false, precision = 20, scale = 8)
    private BigDecimal btcBalance;

    @Column(nullable = false, precision = 20, scale = 8)
    private BigDecimal ethBalance;

    @Enumerated(EnumType.STRING)
    private WalletStatus status;

    @Version
    private Long version;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @CreatedBy
    @Column(updatable = false)
    private String createdBy;

    @LastModifiedBy
    private String lastModifiedBy;

    public boolean hasSufficientBalance(TradingPair pair, TradeType type, BigDecimal amount, BigDecimal price) {
        BigDecimal requiredAmount = price.multiply(amount);

        return switch(pair) {
            case BTCUSDT -> type == TradeType.BUY ?
                    usdtBalance.compareTo(requiredAmount) >= 0 :
                    btcBalance.compareTo(requiredAmount) >= 0;
            case ETHUSDT -> type == TradeType.BUY ?
                    usdtBalance.compareTo(requiredAmount) >= 0 :
                    ethBalance.compareTo(requiredAmount) >= 0;
        };
    }
}
