package org.crypto.assignment.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class BinanceResponse {

    @JsonProperty("symbol")
    private String symbol;

    @JsonProperty("bidPrice")
    private String bidPrice;

    @JsonProperty("askPrice")
    private String askPrice;

}
