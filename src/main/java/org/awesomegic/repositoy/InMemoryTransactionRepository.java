package org.awesomegic.repositoy;

import org.awesomegic.model.Transaction;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public final class InMemoryTransactionRepository implements InMemoryRepository<Transaction,String>, TransactionRepository {
    private final Map<String,Transaction> transactionMap = new ConcurrentHashMap<String, Transaction>();

    @Override
    public Transaction save(Transaction transaction) {

        if(transaction.id() == null || transaction.id().isEmpty()) {
            throw new RuntimeException("Transaction should have ID");
        }

        Transaction newTransaction = new Transaction(
                transaction.id(),
                transaction.date(),
                transaction.accountNumber(),
                transaction.type(),
                transaction.amount(),
                transaction.balance()
        );

        transactionMap.put(transaction.id(), newTransaction);
        return newTransaction;
    }

    @Override
    public Optional<Transaction> findById(String id) {
        return Optional.ofNullable(transactionMap.get(id));
    }

    @Override
    public List<Transaction> findAll() {
        return new ArrayList<>(transactionMap.values());
    }

    @Override
    public boolean deleteById(String id) {
        return transactionMap.remove(id) != null;
    }

    public List<Transaction> findByAccountNumber(String accountNumber) {
        return transactionMap.values().stream()
                .filter(t -> t.accountNumber().equals(accountNumber))
                .collect(Collectors.toList());
    }

    @Override
    public List<Transaction> findByAccountNumberAndDateRange(String accountNumber, LocalDate startDate, LocalDate endDate) {
        return transactionMap.values().stream()
                .filter(t -> t.accountNumber().equals(accountNumber))
                .filter(t -> !t.date().isBefore(startDate) && !t.date().isAfter(endDate))
                .sorted((t1, t2) -> t1.date().compareTo(t2.date()))
                .collect(Collectors.toList());
    }

    @Override
    public List<Transaction> findTransactionsByDate(LocalDate startDate) {
        return transactionMap.values().stream()
                .filter(t -> t.date().isEqual(startDate))
                .collect(Collectors.toList());
    }
}
