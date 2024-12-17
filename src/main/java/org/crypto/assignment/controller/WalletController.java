package org.crypto.assignment.controller;

import lombok.extern.slf4j.Slf4j;
import org.crypto.assignment.model.dto.TradeResponse;
import org.crypto.assignment.model.dto.TradeSearchCriteria;
import org.crypto.assignment.model.dto.WalletBalanceResponse;
import org.crypto.assignment.model.entity.Wallet;
import org.crypto.assignment.service.WalletService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/wallet")
@Slf4j
public class WalletController {

    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @GetMapping("/{walletId}/trades")
    public ResponseEntity<Page<TradeResponse>> getWalletTradeHistory(
            @PathVariable Long walletId,
            @RequestHeader("X-User-ID") Long userId,
            @RequestParam(defaultValue="0") int page,
            @RequestParam(defaultValue="20") int size,
            @RequestParam(required = false, defaultValue = "1") int timePeriodMonths ) {
        log.debug("Fetching trade history for wallet: {}, user: {}, page: {}, size: {}", walletId, userId, page, size);
        TradeSearchCriteria criteria = TradeSearchCriteria.builder()
                .walletId(walletId)
                .userId(userId)
                .timePeriodMonths(timePeriodMonths)
                .build();

        Page<TradeResponse> trades = walletService.getTradeHistory(criteria, page, size);
        return ResponseEntity.ok(trades);
    }

    @GetMapping("/balance")
    public ResponseEntity<WalletBalanceResponse> getWalletBalance(@PathVariable Long walletId,
                                                                  @RequestHeader("X-User-ID") Long userId) {
        Wallet wallet = walletService.getWalletBalance(userId, walletId);
        return ResponseEntity.ok(WalletBalanceResponse.fromWallet(wallet));
    }
}
