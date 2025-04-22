package org.awesomegic.repositoy;

import org.awesomegic.model.Transaction;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository {

    Transaction save(Transaction transaction);

    Optional<Transaction> findById(String id);

    List<Transaction> findAll();

    boolean deleteById(String id);

    List<Transaction> findByAccountNumber(String accountNumber);

    List<Transaction> findByAccountNumberAndDateRange(String accountNumber, LocalDate startDate, LocalDate endDate);
    List<Transaction> findTransactionsByDate(LocalDate startDate);
}
