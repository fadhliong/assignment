package org.awesomegic.service;

import org.awesomegic.model.Account;
import org.awesomegic.model.Transaction;
import org.awesomegic.repositoy.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TransactionServiceTest {
    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AccountService accountService;

    @InjectMocks
    private TransactionService transactionService;

    @Captor
    private ArgumentCaptor<Transaction> transactionCaptor;

    private final String ACCOUNT_NUMBER = "ACC123";
    private final LocalDate TRANSACTION_DATE = LocalDate.of(2025, 1, 15);
    private final String TRANSACTION_INPUT_DEPOSIT = "20250115 ACC123 D 500.00";
    private final String TRANSACTION_INPUT_WITHDRAWAL = "20250115 ACC123 W 200.00";
    private Account testAccount;

    @BeforeEach
    void setUp() {
        testAccount = new Account(ACCOUNT_NUMBER, BigDecimal.valueOf(1000.00), LocalDate.now());
    }

    @Test
    @DisplayName("Should return transactions sorted by date when getTransactionsByAccountNumber is called")
    void shouldReturnTransactionsSortedByDateWhenGetTransactionsByAccountNumberIsCalled() {
        LocalDate date1 = LocalDate.of(2023, 1, 1);
        LocalDate date2 = LocalDate.of(2023, 1, 5);

        List<Transaction> unsortedTransactions = new ArrayList<>();
        unsortedTransactions.add(new Transaction("T2", date2, ACCOUNT_NUMBER, Transaction.TransactionType.DEPOSIT, BigDecimal.TEN, BigDecimal.TEN));
        unsortedTransactions.add(new Transaction("T1", date1, ACCOUNT_NUMBER, Transaction.TransactionType.DEPOSIT, BigDecimal.ONE, BigDecimal.ONE));

        when(transactionRepository.findByAccountNumber(ACCOUNT_NUMBER)).thenReturn(unsortedTransactions);

        List<Transaction> result = transactionService.getTransactionsByAccountNumber(ACCOUNT_NUMBER);

        assertEquals(2, result.size());
        assertEquals(date1, result.get(0).date());
        assertEquals(date2, result.get(1).date());
        verify(transactionRepository).findByAccountNumber(ACCOUNT_NUMBER);
    }

    @Test
    @DisplayName("Should throw exception when transaction input format is invalid")
    void shouldThrowExceptionWhenTransactionInputFormatIsInvalid() {
        String invalidInput = "2023-01-15 ACC123 D";

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                transactionService.processTransaction(invalidInput));
        assertEquals("Invalid input format", exception.getMessage());
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    @DisplayName("Should throw exception when transaction date is in the future")
    void shouldThrowExceptionWhenTransactionDateIsInFuture() {
        LocalDate futureDate = LocalDate.now().plusDays(1);
        String futureDateInput = futureDate.format(DateTimeFormatter.ofPattern("yyyyMMdd")) + " ACC123 D 100.00";

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                transactionService.processTransaction(futureDateInput));
        assertEquals("Transaction date cannot be in the future", exception.getMessage());
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    @DisplayName("Should throw exception when first transaction is withdrawal")
    void shouldThrowExceptionWhenFirstTransactionIsWithdrawal() {
        when(accountService.findOrCreateAccount(ACCOUNT_NUMBER)).thenReturn(testAccount);
        when(accountService.getAccountTransactionCount(ACCOUNT_NUMBER)).thenReturn(0);

        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                transactionService.processTransaction(TRANSACTION_INPUT_WITHDRAWAL));
        assertEquals("First transaction for an account cannot be a withdrawal", exception.getMessage());
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    @DisplayName("Should throw exception when transaction type is invalid")
    void shouldThrowExceptionWhenTransactionTypeIsInvalid() {
        String invalidTypeInput = "20250115 ACC123 X 100.00";

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                transactionService.processTransaction(invalidTypeInput));
        assertTrue(exception.getMessage().contains("Invalid transaction type"));
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    @DisplayName("Should handle transaction repository failure")
    void shouldHandleTransactionRepositoryFailure() {
        when(accountService.findOrCreateAccount(ACCOUNT_NUMBER)).thenReturn(testAccount);
        when(accountService.getAccountTransactionCount(ACCOUNT_NUMBER)).thenReturn(1);
        when(transactionRepository.findTransactionsByDate(TRANSACTION_DATE)).thenReturn(new ArrayList<>());
        when(transactionRepository.save(any(Transaction.class))).thenThrow(new RuntimeException("Database error"));

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                transactionService.processTransaction(TRANSACTION_INPUT_DEPOSIT));
        assertEquals("Transaction creation failed", exception.getMessage());
    }
}
