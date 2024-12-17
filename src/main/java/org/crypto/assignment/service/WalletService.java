package org.crypto.assignment.service;

import lombok.extern.slf4j.Slf4j;
import org.crypto.assignment.exception.WalletAccessDeniedException;
import org.crypto.assignment.logger.AuditLogger;
import org.crypto.assignment.model.dto.TradeResponse;
import org.crypto.assignment.model.dto.TradeSearchCriteria;
import org.crypto.assignment.model.entity.Trade;
import org.crypto.assignment.model.entity.Wallet;
import org.crypto.assignment.repository.TradeRepository;
import org.crypto.assignment.repository.WalletRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.Optional;

@Service
@Slf4j
@Transactional(readOnly = true)
public class WalletService {

    private final WalletRepository walletRepository;
    private final TradeRepository tradeRepository;
    private final AuditLogger auditLogger;


    public WalletService(WalletRepository walletRepository, TradeRepository tradeRepository, AuditLogger auditLogger) {
        this.walletRepository = walletRepository;
        this.tradeRepository = tradeRepository;
        this.auditLogger = auditLogger;
    }


    public Wallet getWalletBalance(Long userId, Long walletId) {
        try{
            validateWallet(userId, walletId);
            auditLogger.log("WALLET_BALANCE_CHECK", userId, walletId);

            Optional<Wallet> balance = this.walletRepository.findWalletByWalletId(walletId);
            if(balance.isPresent()) {
                return balance.get();
            } else {
                auditLogger.log("WALLET_ACCESS_PROBLEM", userId, walletId);
                throw new RuntimeException("Something went wrong. Please try again later.");
            }
        } catch(Exception e) {
            log.error(e.getMessage());
        }

        return null;
    }

    public Page<TradeResponse> getTradeHistory(TradeSearchCriteria criteria, int page, int size) {
        Long userId = criteria.getUserId();
        Long walletId = criteria.getWalletId();

        validateWallet(userId, walletId);
        validatePageParameters(page,size);

        auditLogger.log("TRADE_HISTORY_ACCESS", criteria.getUserId(), criteria.getWalletId());

        LocalDateTime startSearchDate = LocalDate.now()
                .minusMonths(criteria.getTimePeriodMonths())
                .with(TemporalAdjusters.firstDayOfMonth())
                .atStartOfDay();
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<Trade> trades = tradeRepository.findByWalletIdAndCreatedAtAfterOrderByCreatedAt(walletId, startSearchDate, pageable);
        return trades.map(TradeResponse::fromTrade);

    }

    private void validateWallet(Long userId, Long walletId) {
        if(walletRepository.findByUserIdAndWalletId(userId, walletId).isEmpty()) {
            auditLogger.log("WALLET_ACCESS_DENIED", userId, walletId);
            throw new WalletAccessDeniedException("User does not have access to this wallet");
        }
    }

    private void validatePageParameters(int page, int size) {
        if (page < 0) {
            throw new IllegalArgumentException("Page index must not be less than zero");
        }
        if (size < 1) {
            throw new IllegalArgumentException("Page size must not be less than one");
        }
        if (size > 100) {
            throw new IllegalArgumentException("Page size must not be greater than 100");
        }
    }
}
