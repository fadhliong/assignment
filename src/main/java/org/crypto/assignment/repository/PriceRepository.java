package org.crypto.assignment.repository;

import org.crypto.assignment.model.entity.Price;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PriceRepository extends JpaRepository<Price, Long> {
    Optional<Price> findTopByOrderByTimestampDesc();
}
