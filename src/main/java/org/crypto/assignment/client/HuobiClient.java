package org.crypto.assignment.client;

import lombok.extern.slf4j.Slf4j;
import org.crypto.assignment.exception.ApiClientException;
import org.crypto.assignment.model.dto.HuobiPrice;
import org.crypto.assignment.model.dto.HuobiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Slf4j
@Component
public class HuobiClient extends BaseApiClient {

    private final String baseUrl;
    private final String tickerPath;

    @Autowired
    public HuobiClient(RestTemplate restTemplate,
                       @Value("${exchange.huobi.base-url}") String baseUrl,
                       @Value("${exchange.huobi.ticker-path}") String tickerPath) {
        super(restTemplate);
        this.baseUrl = baseUrl;
        this.tickerPath = tickerPath;
    }

    public List<HuobiPrice> fetchTickerData() {
        try {
            String url = baseUrl + tickerPath;
            return restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    createRequestEntity(),
                    HuobiResponse.class
            ).getBody().getData();
        } catch (Exception e) {
            log.error("Error fetching Huobi ticker data", e);
            throw new ApiClientException("Failed to fetch Huobi data", e);
        }
    }
}
