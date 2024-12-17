package org.crypto.assignment.model.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TradeSearchCriteria {
    private Long walletId;
    private Long userId;
    private int timePeriodMonths;
}
