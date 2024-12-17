package org.crypto.assignment.service;

import org.crypto.assignment.client.BinanceClient;
import org.crypto.assignment.client.HuobiClient;
import org.crypto.assignment.model.dto.BinanceResponse;
import org.crypto.assignment.model.dto.HuobiPrice;
import org.crypto.assignment.model.entity.Price;
import org.crypto.assignment.repository.PriceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PriceResponseServiceTest {

    @Mock
    private BinanceClient binanceClient;
    @Mock
    private HuobiClient huobiClient;
    @Mock
    private PriceRepository priceRepository;

    private PriceResponseService priceResponseService;

    @BeforeEach
    void setUp() {
        priceResponseService = new PriceResponseService(binanceClient, huobiClient, priceRepository);
    }

    @Nested
    @DisplayName("getLatestPriceResponse Tests")
    class GetLatestPriceResponseTests {

        @Test
        @DisplayName("Should return latest price when data exists")
        void shouldReturnLatestPrice() {
            Price expectedPrice = Price.builder()
                    .ethusdtBestBid(BigDecimal.valueOf(2000))
                    .ethusdtBestAsk(BigDecimal.valueOf(2001))
                    .btcusdtBestBid(BigDecimal.valueOf(40000))
                    .btcusdtBestAsk(BigDecimal.valueOf(40001))
                    .timestamp(LocalDateTime.now())
                    .build();

            when(priceRepository.findTopByOrderByTimestampDesc())
                    .thenReturn(Optional.of(expectedPrice));

            Price result = priceResponseService.getLatestPriceResponse();

            assertThat(result).isEqualTo(expectedPrice);
            verify(priceRepository).findTopByOrderByTimestampDesc();
        }

        @Test
        @DisplayName("Should throw exception when no price data exists")
        void shouldThrowExceptionWhenNoPriceExists() {
            when(priceRepository.findTopByOrderByTimestampDesc())
                    .thenReturn(Optional.empty());

            assertThrows(RuntimeException.class,
                    () -> priceResponseService.getLatestPriceResponse(),
                    "No price data available");
        }
    }

    @Nested
    @DisplayName("fetchAndSavePrices Tests")
    class FetchAndSavePricesTests {

        @Test
        @DisplayName("Should save best prices from both exchanges")
        void shouldSaveBestPricesFromBothExchanges() throws InterruptedException {
            BinanceResponse binanceEthResponse = new BinanceResponse();
            binanceEthResponse.setSymbol("ETHUSDT");
            binanceEthResponse.setBidPrice("2000.00");
            binanceEthResponse.setAskPrice("2001.00");

            BinanceResponse binanceBtcResponse = new BinanceResponse();
            binanceBtcResponse.setSymbol("BTCUSDT");
            binanceBtcResponse.setBidPrice("40000.00");
            binanceBtcResponse.setAskPrice("40001.00");

            HuobiPrice huobiEthPrice = new HuobiPrice();
            huobiEthPrice.setSymbol("ETHUSDT");
            huobiEthPrice.setBid(BigDecimal.valueOf(2000.50));
            huobiEthPrice.setAsk(BigDecimal.valueOf(2001.50));

            HuobiPrice huobiBtcPrice = new HuobiPrice();
            huobiBtcPrice.setSymbol("BTCUSDT");
            huobiBtcPrice.setBid(BigDecimal.valueOf(40000.50));
            huobiBtcPrice.setAsk(BigDecimal.valueOf(40001.50));

            when(binanceClient.fetchTickerData())
                    .thenReturn(List.of(binanceEthResponse, binanceBtcResponse));
            when(huobiClient.fetchTickerData())
                    .thenReturn(List.of(huobiEthPrice, huobiBtcPrice));

            priceResponseService.fetchAndSavePrices();

            TimeUnit.SECONDS.sleep(1);

            verify(priceRepository, timeout(5000)).save(argThat(price ->
                    price.getEthusdtBestBid().compareTo(BigDecimal.valueOf(2000.50)) == 0 &&
                            price.getEthusdtBestAsk().compareTo(BigDecimal.valueOf(2001.00)) == 0 &&
                            price.getBtcusdtBestBid().compareTo(BigDecimal.valueOf(40000.50)) == 0 &&
                            price.getBtcusdtBestAsk().compareTo(BigDecimal.valueOf(40001.00)) == 0
            ));
        }

        @Test
        @DisplayName("Should handle empty responses from exchanges")
        void shouldHandleEmptyResponses() throws InterruptedException {
            when(binanceClient.fetchTickerData()).thenReturn(List.of());
            when(huobiClient.fetchTickerData()).thenReturn(List.of());

            priceResponseService.fetchAndSavePrices();

            TimeUnit.SECONDS.sleep(1);

            verify(priceRepository, timeout(5000)).save(argThat(price ->
                    price.getEthusdtBestBid().compareTo(BigDecimal.ZERO) == 0 &&
                            price.getEthusdtBestAsk().compareTo(BigDecimal.ZERO) == 0 &&
                            price.getBtcusdtBestBid().compareTo(BigDecimal.ZERO) == 0 &&
                            price.getBtcusdtBestAsk().compareTo(BigDecimal.ZERO) == 0
            ));
        }

        @Test
        @DisplayName("Should handle client exceptions gracefully")
        void shouldHandleClientExceptions() throws InterruptedException {
            when(binanceClient.fetchTickerData()).thenThrow(new RuntimeException("API Error"));

            priceResponseService.fetchAndSavePrices();

            TimeUnit.SECONDS.sleep(1);

            verify(priceRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Price Conversion Tests")
    class PriceConversionTests {

        @Test
        @DisplayName("Should convert Binance response correctly")
        void shouldConvertBinanceResponse() throws InterruptedException {
            BinanceResponse binanceResponse = new BinanceResponse();
            binanceResponse.setSymbol("ETHUSDT");
            binanceResponse.setBidPrice("2000.00");
            binanceResponse.setAskPrice("2001.00");

            when(binanceClient.fetchTickerData())
                    .thenReturn(List.of(binanceResponse));
            when(huobiClient.fetchTickerData())
                    .thenReturn(List.of());

            priceResponseService.fetchAndSavePrices();

            TimeUnit.SECONDS.sleep(1);

            verify(priceRepository, timeout(5000)).save(argThat(price ->
                    price.getEthusdtBestBid().compareTo(new BigDecimal("2000.00")) == 0 &&
                            price.getEthusdtBestAsk().compareTo(new BigDecimal("2001.00")) == 0
            ));
        }

        @Test
        @DisplayName("Should convert Huobi response correctly")
        void shouldConvertHuobiResponse() throws InterruptedException {
            HuobiPrice huobiPrice = new HuobiPrice();
            huobiPrice.setSymbol("ETHUSDT");
            huobiPrice.setBid(BigDecimal.valueOf(2000.00));
            huobiPrice.setAsk(BigDecimal.valueOf(2001.00));

            when(binanceClient.fetchTickerData())
                    .thenReturn(List.of());
            when(huobiClient.fetchTickerData())
                    .thenReturn(List.of(huobiPrice));

            priceResponseService.fetchAndSavePrices();

            TimeUnit.SECONDS.sleep(1);

            verify(priceRepository, timeout(5000)).save(argThat(price ->
                    price.getEthusdtBestBid().compareTo(new BigDecimal("2000.00")) == 0 &&
                            price.getEthusdtBestAsk().compareTo(new BigDecimal("2001.00")) == 0
            ));
        }
    }
}
