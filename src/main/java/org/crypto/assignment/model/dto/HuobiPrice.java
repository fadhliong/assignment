package org.crypto.assignment.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class HuobiPrice {

    @JsonProperty("symbol")
    private String symbol;

    @JsonProperty("bid")
    private BigDecimal bid;

    @JsonProperty("bidSize")
    private BigDecimal bidSize;

    @JsonProperty("ask")
    private BigDecimal ask;

    @JsonProperty("askSize")
    private BigDecimal askSize;
}
