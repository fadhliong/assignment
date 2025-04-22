package org.awesomegic.service;

import org.awesomegic.model.Account;
import org.awesomegic.model.Transaction;
import org.awesomegic.repositoy.TransactionRepository;
import org.awesomegic.util.InputValidator;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;


public class TransactionService {

    public record TransactionRequest(
            LocalDate date,
            String accountNumber,
            String transactionType,
            BigDecimal amount
    ) {}

    private final TransactionRepository transactionRepository;
    private final AccountService accountService;

    public TransactionService(
            TransactionRepository transactionRepository,
            AccountService accountService) {
        this.transactionRepository = transactionRepository;
        this.accountService = accountService;
    }

    public List<Transaction> getTransactionsByAccountNumber(String accountNumber) {
        List<Transaction> transactions = transactionRepository.findByAccountNumber(accountNumber);
        transactions.sort(Comparator.comparing(Transaction::date).thenComparing(Transaction::id));

        return transactions;
    }

    public void deleteById(String accountNumber) {
        transactionRepository.deleteById(accountNumber);
    }

    public Transaction save(Transaction transaction) {
        transactionRepository.save(transaction);
        return transaction;
    }

    public List<Transaction> findByAccountNumberAndDateRange(String accountNumber, LocalDate startDate, LocalDate endDate) {
        return transactionRepository.findByAccountNumberAndDateRange(accountNumber, startDate, endDate);
    }

    public Transaction processTransaction(String input) {
        TransactionRequest request = parseTransactionInput(input);

        validateTransactionRequest(request);

        Transaction.TransactionType transactionType =
                parseTransactionType(request.transactionType());

        String transactionId = generateTransactionId(request.date());

        Account account = accountService.findOrCreateAccount(request.accountNumber());
        validateAccountTransaction(account, transactionType, request.amount());
        BigDecimal currentBalance = account.balance();
        BigDecimal newBalance = calculateNewBalance(currentBalance, transactionType, request.amount());

        Transaction transaction = new Transaction(
                transactionId,
                request.date(),
                request.accountNumber(),
                transactionType,
                request.amount(),
                newBalance
        );

        Transaction savedTransaction = null;

        try {
            savedTransaction = transactionRepository.save(transaction);

        } catch (Exception e) {
            if(savedTransaction != null) {
                try {
                    transactionRepository.deleteById(transactionId);
                } catch (Exception ex) {
                    throw new RuntimeException("Transaction has failed and has been rolled back. Please try again");
                }
            }
            throw new RuntimeException("Transaction creation failed");
        }
        return transaction;
    }

    private TransactionRequest parseTransactionInput(String input) {
        String[] parts = input.split("\\s+");
        if (parts.length != 4) {
            throw new IllegalArgumentException("Invalid input format");
        }

        return new TransactionRequest(
                InputValidator.parseAndValidateDate(parts[0]),
                parts[1],
                parts[2].toUpperCase(),
                new BigDecimal(parts[3])
        );
    }

    private void validateTransactionRequest(TransactionRequest request) {
        if (request.date() == null) {
            throw new IllegalArgumentException("Transaction date cannot be null");
        }
        if (request.accountNumber() == null || request.accountNumber().isBlank()) {
            throw new IllegalArgumentException("Account number cannot be empty");
        }
        if (request.transactionType() == null) {
            throw new IllegalArgumentException("Transaction type cannot be null");
        }
        if (request.amount() == null) {
            throw new IllegalStateException("Transaction amount cannot be null");
        }

        if (request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("Transaction amount must be positive");
        }

        if (request.date().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Transaction date cannot be in the future");
        }
    }

    private void validateAccountTransaction(
            Account account,
            Transaction.TransactionType transactionType,
            BigDecimal amount) {
        if (accountService.getAccountTransactionCount(account.accountNumber()) == 0
                && transactionType == Transaction.TransactionType.WITHDRAWAL) {
            throw new IllegalStateException("First transaction for an account cannot be a withdrawal");
        }

        if (transactionType == Transaction.TransactionType.WITHDRAWAL) {
            BigDecimal currentBalance = accountService.getAccountBalance(account.accountNumber());
            if (currentBalance.subtract(amount).compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalStateException("Insufficient funds for withdrawal");
            }
        }
    }

    private Transaction.TransactionType parseTransactionType(String typeInput) {
        try {
            return Transaction.TransactionType.fromCode(typeInput.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid transaction type: " + typeInput);
        }
    }

    private String generateTransactionId(LocalDate date) {
        String datePrefix = date.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        int uniqueDateSuffix = transactionRepository.findTransactionsByDate(date).size() + 1;
        return datePrefix + "-" + String.format("%02d",uniqueDateSuffix);
    }

    private BigDecimal calculateNewBalance(
            BigDecimal currentBalance,
            Transaction.TransactionType transactionType,
            BigDecimal amount) {
        return switch (transactionType) {
            case DEPOSIT, INTEREST -> currentBalance.add(amount);
            case WITHDRAWAL -> currentBalance.subtract(amount);
        };
    }

}
