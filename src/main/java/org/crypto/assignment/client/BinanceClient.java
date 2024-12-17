package org.crypto.assignment.client;

import lombok.extern.slf4j.Slf4j;
import org.crypto.assignment.exception.ApiClientException;
import org.crypto.assignment.model.dto.BinanceResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Slf4j
@Component
public class BinanceClient extends BaseApiClient {

    private final String baseUrl;
    private final String tickerPath;

    @Autowired
    public BinanceClient(RestTemplate restTemplate,
                         @Value("${exchange.binance.base-url}") String baseUrl,
                         @Value("${exchange.binance.ticker-path}") String tickerPath) {
        super(restTemplate);
        this.baseUrl = baseUrl;
        this.tickerPath = tickerPath;
    }

    public List<BinanceResponse> fetchTickerData() {
        try {
            String url = baseUrl + tickerPath;
            return restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    createRequestEntity(),
                    new ParameterizedTypeReference<List<BinanceResponse>>() {}
            ).getBody();
        } catch (Exception e) {
            log.error("Error fetching Binance ticker data", e);
            throw new ApiClientException("Failed to fetch Binance data", e);
        }
    }
}
