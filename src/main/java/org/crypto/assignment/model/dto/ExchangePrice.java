package org.crypto.assignment.model.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class ExchangePrice {
    private String symbol;
    private BigDecimal bidPrice;
    private BigDecimal askPrice;
}