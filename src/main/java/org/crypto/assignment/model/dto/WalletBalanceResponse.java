package org.crypto.assignment.model.dto;

import lombok.Builder;
import lombok.Value;
import org.crypto.assignment.model.entity.Wallet;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Value
@Builder
public class WalletBalanceResponse {
    Long walletId;
    BigDecimal usdtBalance;
    BigDecimal btcBalance;
    BigDecimal ethBalance;
    LocalDateTime lastUpdated;

    public static WalletBalanceResponse fromWallet(Wallet wallet) {
        return WalletBalanceResponse.builder()
                .walletId(wallet.getId())
                .usdtBalance(wallet.getUsdtBalance())
                .btcBalance(wallet.getBtcBalance())
                .ethBalance(wallet.getEthBalance())
                .lastUpdated(wallet.getUpdatedAt())
                .build();
    }
}
