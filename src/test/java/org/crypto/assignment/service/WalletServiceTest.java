package org.crypto.assignment.service;

import org.crypto.assignment.logger.AuditLogger;
import org.crypto.assignment.model.entity.Wallet;
import org.crypto.assignment.repository.TradeRepository;
import org.crypto.assignment.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WalletServiceTest {

    @Mock
    private WalletRepository walletRepository;
    @Mock
    private TradeRepository tradeRepository;
    @Mock
    private AuditLogger auditLogger;

    private WalletService walletService;

    private static final Long USER_ID = 1L;
    private static final Long WALLET_ID = 1L;

    @BeforeEach
    void setUp() {
        walletService = new WalletService(walletRepository, tradeRepository, auditLogger);
    }

    @Nested
    @DisplayName("Wallet Balance Tests")
    class WalletBalanceTests {
        @Test
        @DisplayName("Should return wallet balance when user has access")
        void shouldReturnWalletBalanceWhenUserHasAccess() {
            Wallet expectedWallet = createWallet();
            when(walletRepository.findByUserIdAndWalletId(USER_ID, WALLET_ID))
                    .thenReturn(Optional.of(expectedWallet));
            when(walletRepository.findWalletByWalletId(WALLET_ID))
                    .thenReturn(Optional.of(expectedWallet));

            Wallet result = walletService.getWalletBalance(USER_ID, WALLET_ID);

            assertThat(result).isNotNull()
                    .isEqualTo(expectedWallet);
            verify(auditLogger).log("WALLET_BALANCE_CHECK", USER_ID, WALLET_ID);
        }

        @Test
        @DisplayName("Should handle wallet not found after validation")
        void shouldHandleWalletNotFound() {
            when(walletRepository.findByUserIdAndWalletId(USER_ID, WALLET_ID))
                    .thenReturn(Optional.of(createWallet()));
            when(walletRepository.findWalletByWalletId(WALLET_ID))
                    .thenReturn(Optional.empty());

            Wallet result = walletService.getWalletBalance(USER_ID, WALLET_ID);

            assertThat(result).isNull();
            verify(auditLogger).log("WALLET_ACCESS_PROBLEM", USER_ID, WALLET_ID);
        }
    }

    private Wallet createWallet() {
        return Wallet.builder()
                .id(WALLET_ID)
                .userId(USER_ID)
                .usdtBalance(BigDecimal.valueOf(1000))
                .btcBalance(BigDecimal.ONE)
                .ethBalance(BigDecimal.ONE)
                .build();
    }
}