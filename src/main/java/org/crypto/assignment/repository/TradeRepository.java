package org.crypto.assignment.repository;

import org.crypto.assignment.model.entity.Trade;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface TradeRepository extends JpaRepository<Trade,Long> {

    Optional<Trade> findByIdempotencyKey(String idempotencyKey);

    @Query("SELECT t FROM Trade t JOIN t.wallet w WHERE w.walletId = :walletId AND t.createdAt > :createdAt ORDER BY t.createdAt DESC")
    Page<Trade> findByWalletIdAndCreatedAtAfterOrderByCreatedAt(
            @Param("walletId") Long walletId,
            @Param("createdAt") LocalDateTime createdAt,
            Pageable pageable
    );
}
