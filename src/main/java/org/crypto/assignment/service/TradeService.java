package org.crypto.assignment.service;

import lombok.extern.slf4j.Slf4j;
import org.crypto.assignment.exception.InsufficientBalanceException;
import org.crypto.assignment.exception.PriceValidationException;
import org.crypto.assignment.exception.WalletException;
import org.crypto.assignment.logger.AuditLogger;
import org.crypto.assignment.model.dto.TradeRequest;
import org.crypto.assignment.model.dto.TradeResponse;
import org.crypto.assignment.model.entity.Price;
import org.crypto.assignment.model.entity.Trade;
import org.crypto.assignment.model.entity.Wallet;
import org.crypto.assignment.model.enums.TradeStatus;
import org.crypto.assignment.model.enums.TradeType;
import org.crypto.assignment.model.enums.TradingPair;
import org.crypto.assignment.repository.TradeRepository;
import org.crypto.assignment.repository.WalletRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@Slf4j
public class TradeService {
    private final WalletRepository walletRepository;
    private final TradeRepository tradeRepository;
    private final PriceResponseService priceService;
    private final WalletService walletService;
    private final TransactionTemplate transactionTemplate;
    private final AuditLogger auditLogger;

    public TradeService(WalletRepository walletRepository, TradeRepository tradeRepository, PriceResponseService priceService, WalletService walletService, TransactionTemplate transactionTemplate, AuditLogger auditLogger) {
        this.walletRepository = walletRepository;
        this.tradeRepository = tradeRepository;
        this.priceService = priceService;
        this.walletService = walletService;
        this.transactionTemplate = transactionTemplate;
        this.auditLogger = auditLogger;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public TradeResponse executeTrade(TradeRequest request) {
        Optional<Trade> existingTrade = tradeRepository.findByIdempotencyKey(request.getIdempotencyKey());
        if (existingTrade.isPresent()) {
            log.info("Returning existing trade for idempotency key: {}", request.getIdempotencyKey());
            return TradeResponse.fromTrade(existingTrade.get());
        }

        Price latestPrice = priceService.getLatestPriceResponse();

        BigDecimal marketPrice = getMarketPrice(
                request.getTradingPair(),
                request.getTradeType(),
                latestPrice
        );

        validateTradePrice(request, marketPrice);

        Trade trade = createInitialTradeRecord(request, marketPrice);
        trade = tradeRepository.save(trade);

        try {
            return executeTradeTransaction(trade, request, latestPrice);
        } catch (Exception e) {
            handleTradeFailure(trade, e);
            throw e;
        }
    }

    private void validateTradePrice(TradeRequest request, BigDecimal marketPrice) {

        BigDecimal requestedPrice = request.getPrice();
        boolean isPriceValid;
        String errorDetail;

        if (request.getTradeType() == TradeType.BUY) {
            isPriceValid = requestedPrice.compareTo(marketPrice) >= 0;
            errorDetail = String.format(
                    "Buy price %s is too low. Market ask: %s",
                    requestedPrice,
                    marketPrice
            );
        } else {
            isPriceValid = requestedPrice.compareTo(marketPrice) <= 0;
            errorDetail = String.format(
                    "Sell price %s is too low. Market bid: %s",
                    requestedPrice,
                    marketPrice
            );
        }

        if (!isPriceValid) {
            throw new PriceValidationException(String.format(
                    "Price validation failed for %s %s. %s",
                    request.getTradeType(),
                    request.getTradingPair(),
                    errorDetail
            ));
        }
    }

    private BigDecimal getMarketPrice(TradingPair tradingPair, TradeType tradeType, Price latestPrice) {
        return switch (tradingPair) {
            case BTCUSDT -> tradeType == TradeType.BUY ?
                    latestPrice.getBtcusdtBestAsk() :
                    latestPrice.getBtcusdtBestBid();
            case ETHUSDT -> tradeType == TradeType.BUY ?
                    latestPrice.getEthusdtBestAsk() :
                    latestPrice.getEthusdtBestBid();
        };
    }

    private Trade createInitialTradeRecord(TradeRequest request, BigDecimal marketPrice) {
        return Trade.builder()
                .idempotencyKey(request.getIdempotencyKey())
                .tradingPair(request.getTradingPair())
                .tradeType(request.getTradeType())
                .amount(request.getAmount())
                .price(request.getPrice())
                .wallet(walletService.getWalletBalance(request.getUserId(), request.getWalletId()))
                .marketPrice(marketPrice)
                .totalMarketValue(request.getAmount().multiply(marketPrice))
                .status(TradeStatus.PENDING)
                .build();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected TradeResponse executeTradeTransaction(Trade trade, TradeRequest request, Price latestPrice) {
        return transactionTemplate.execute(status -> {
            try {
                Wallet wallet = walletRepository.findByWalletIdWithLock(request.getWalletId())
                        .orElseThrow(() -> new WalletException("Wallet not found: " + request.getWalletId()));

                BigDecimal executionPrice = getMarketPrice(request.getTradingPair(), request.getTradeType(), latestPrice);
                BigDecimal totalValue = request.getAmount().multiply(executionPrice);

                if (!wallet.hasSufficientBalance(
                        request.getTradingPair(),
                        request.getTradeType(),
                        request.getAmount(),
                        executionPrice)) {
                    throw new InsufficientBalanceException(
                            String.format("Insufficient balance for trade. Required: %s, Available: %s",
                                    totalValue,
                                    getAvailableBalance(wallet, request.getTradingPair(), request.getTradeType()))
                    );
                }

                updateWalletBalances(wallet, request, executionPrice);
                walletRepository.save(wallet);

                updateTradeWithExecutionDetails(trade, wallet, executionPrice);
                Trade completedTrade = tradeRepository.save(trade);

                log.info("Trade executed successfully: {} {} {} at price {}",
                        request.getTradeType(),
                        request.getAmount(),
                        request.getTradingPair(),
                        executionPrice
                );

                return TradeResponse.fromTrade(completedTrade);
            } catch (Exception e) {
                status.setRollbackOnly();
                throw e;
            }
        });
    }

    private void updateTradeWithExecutionDetails(Trade trade, Wallet wallet, BigDecimal executionPrice) {
        wallet.setUpdatedAt(LocalDateTime.now());
        wallet.setVersion(wallet.getVersion()+1);
        wallet.setLastModifiedBy("System");
        trade.setWallet(wallet);
        trade.setStatus(TradeStatus.COMPLETED);
        trade.setExecutionPrice(executionPrice);
        trade.setExecutedAt(LocalDateTime.now());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected void handleTradeFailure(Trade trade, Exception e) {
        try {
            trade.setStatus(TradeStatus.FAILED);
            trade.setFailureReason(getFailureReason(e));
            tradeRepository.save(trade);
        } catch (Exception ex) {
            log.error("Failed to update trade status for failed trade {}", trade.getId(), ex);
        }
    }

    private String getFailureReason(Exception e) {
        if (e instanceof InsufficientBalanceException) {
            return "Insufficient balance";
        } else if (e instanceof WalletException) {
            return "Wallet not found";
        } else {
            return "Internal error: " + e.getMessage();
        }
    }

    private void updateWalletBalances(Wallet wallet, TradeRequest request, BigDecimal executionPrice) {
        BigDecimal tradeValue = request.getAmount().multiply(executionPrice);

        switch (request.getTradingPair()) {
            case BTCUSDT -> {
                if (request.getTradeType() == TradeType.BUY) {
                    wallet.setUsdtBalance(wallet.getUsdtBalance().subtract(tradeValue));
                    wallet.setBtcBalance(wallet.getBtcBalance().add(request.getAmount()));
                } else {
                    wallet.setBtcBalance(wallet.getBtcBalance().subtract(request.getAmount()));
                    wallet.setUsdtBalance(wallet.getUsdtBalance().add(tradeValue));
                }
            }
            case ETHUSDT -> {
                if (request.getTradeType() == TradeType.BUY) {
                    wallet.setUsdtBalance(wallet.getUsdtBalance().subtract(tradeValue));
                    wallet.setEthBalance(wallet.getEthBalance().add(request.getAmount()));
                } else {
                    wallet.setEthBalance(wallet.getEthBalance().subtract(request.getAmount()));
                    wallet.setUsdtBalance(wallet.getUsdtBalance().add(tradeValue));
                }
            }
        }
    }

    private BigDecimal getAvailableBalance(Wallet wallet, TradingPair tradingPair, TradeType tradeType) {
        return switch (tradingPair) {
            case BTCUSDT -> tradeType == TradeType.BUY ? wallet.getUsdtBalance() : wallet.getBtcBalance();
            case ETHUSDT -> tradeType == TradeType.BUY ? wallet.getUsdtBalance() : wallet.getEthBalance();
        };
    }
}
