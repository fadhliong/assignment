package org.crypto.assignment.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "price", indexes = {
        @Index(name = "idx_ethusdtBestBid", columnList = "ethusdtBestBid"),
        @Index(name = "idx_ethusdtBestAsk", columnList = "ethusdtBestAsk"),
        @Index(name = "idx_btcusdtBestBid", columnList = "btcusdtBestBid"),
        @Index(name = "idx_btcusdtBestAsk", columnList = "btcusdtBestAsk"),
})
public class Price {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private BigDecimal ethusdtBestBid;
    private BigDecimal ethusdtBestAsk;
    private BigDecimal btcusdtBestBid;
    private BigDecimal btcusdtBestAsk;
    private LocalDateTime timestamp;
}
