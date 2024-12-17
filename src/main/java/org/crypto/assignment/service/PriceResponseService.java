package org.crypto.assignment.service;

import lombok.extern.slf4j.Slf4j;
import org.crypto.assignment.client.BinanceClient;
import org.crypto.assignment.client.HuobiClient;
import org.crypto.assignment.model.dto.ExchangePrice;
import org.crypto.assignment.model.dto.BinanceResponse;
import org.crypto.assignment.model.dto.HuobiPrice;
import org.crypto.assignment.model.entity.Price;
import org.crypto.assignment.model.enums.TradingPair;
import org.crypto.assignment.repository.PriceRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Slf4j
public class PriceResponseService {

    private static final List<String> TARGET_SYMBOLS = List.of("ETHUSDT", "BTCUSDT");

    private final BinanceClient binanceClient;
    private final HuobiClient huobiClient;
    private final PriceRepository repository;

    public PriceResponseService(BinanceClient binanceClient, HuobiClient huobiClient, PriceRepository repository) {
        this.binanceClient = binanceClient;
        this.huobiClient = huobiClient;
        this.repository = repository;
    }

    public Price getLatestPriceResponse() {
        return repository.findTopByOrderByTimestampDesc()
                .orElseThrow(() -> new RuntimeException("No price data available"));
    }

    @Scheduled(fixedRate = 600000)
    public void fetchAndSavePrices() {
        try {
            CompletableFuture<List<BinanceResponse>> binanceFuture = CompletableFuture.supplyAsync(binanceClient::fetchTickerData);
            CompletableFuture<List<HuobiPrice>> huobiFuture = CompletableFuture.supplyAsync(huobiClient::fetchTickerData);

            CompletableFuture.allOf(binanceFuture, huobiFuture)
                    .thenRun(() -> {
                        try {
                            Map<String, ExchangePrice> binancePrices = binanceFuture.join().stream()
                                    .collect(Collectors.toMap(
                                            BinanceResponse::getSymbol,
                                            this::convertBinanceData
                                    ));

                            Map<String, ExchangePrice> huobiPrices = huobiFuture.join().stream()
                                    .filter(priceDTO -> TARGET_SYMBOLS.contains(priceDTO.getSymbol().toUpperCase()))
                                    .collect(Collectors.toMap(
                                            HuobiPrice::getSymbol,
                                            this::convertHuobiData
                                    ));

                            BigDecimal ethusdtBestBid = getBestBid(TradingPair.ETHUSDT.name(), binancePrices, huobiPrices);
                            BigDecimal ethusdtBestAsk = getBestAsk(TradingPair.ETHUSDT.name(), binancePrices, huobiPrices);
                            BigDecimal btcusdtBestBid = getBestBid(TradingPair.BTCUSDT.name(), binancePrices, huobiPrices);
                            BigDecimal btcusdtBestAsk = getBestAsk(TradingPair.BTCUSDT.name(), binancePrices, huobiPrices);

                            Price priceResponse = Price.builder()
                                    .ethusdtBestBid(ethusdtBestBid)
                                    .ethusdtBestAsk(ethusdtBestAsk)
                                    .btcusdtBestBid(btcusdtBestBid)
                                    .btcusdtBestAsk(btcusdtBestAsk)
                                    .timestamp(LocalDateTime.now())
                                    .build();

                            repository.save(priceResponse);
                            log.info("PriceResponse saved successfully: {}", priceResponse);

                        } catch (Exception e) {
                            log.error("Error processing price data", e);
                        }
                    })
                    .exceptionally(ex -> {
                        log.error("Error during price fetch", ex);
                        return null;
                    });
        } catch (Exception e) {
            log.error("Unexpected error during scheduled price fetch", e);
        }
    }


    private BigDecimal getBestBid(String symbol, Map<String, ExchangePrice> binancePrices, Map<String, ExchangePrice> huobiPrices) {
        return Stream.of(binancePrices.get(symbol), huobiPrices.get(symbol))
                .filter(Objects::nonNull)
                .map(ExchangePrice::getBidPrice)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
    }

    private BigDecimal getBestAsk(String symbol, Map<String, ExchangePrice> binancePrices, Map<String, ExchangePrice> huobiPrices) {
        return Stream.of(binancePrices.get(symbol), huobiPrices.get(symbol))
                .filter(Objects::nonNull)
                .map(ExchangePrice::getAskPrice)
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
    }

    private ExchangePrice convertBinanceData(BinanceResponse data) {
        return ExchangePrice.builder()
                .symbol(data.getSymbol())
                .bidPrice(new BigDecimal(data.getBidPrice()))
                .askPrice(new BigDecimal(data.getAskPrice()))
                .build();
    }

    private ExchangePrice convertHuobiData(HuobiPrice data) {
        return ExchangePrice.builder()
                .symbol(data.getSymbol())
                .bidPrice(data.getBid())
                .askPrice(data.getAsk())
                .build();
    }
}
