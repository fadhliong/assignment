package org.awesomegic.service;

import org.awesomegic.model.Account;
import org.awesomegic.model.Transaction;
import org.awesomegic.repositoy.AccountRepository;
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
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AccountServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private AccountService accountService;

    @Captor
    private ArgumentCaptor<Account> accountCaptor;

    private final String ACCOUNT_NUMBER = "ACC123";
    private final BigDecimal INITIAL_BALANCE = BigDecimal.valueOf(1000.00);
    private final LocalDate CREATED_DATE = LocalDate.now();

    private Account testAccount;

    @BeforeEach
    void setUp() {
        testAccount = new Account(ACCOUNT_NUMBER, INITIAL_BALANCE, CREATED_DATE);
    }

    @Test
    @DisplayName("Should return existing account when findOrCreateAccount is called with an existing account number")
    void shouldReturnExistingAccountWhenFindOrCreateAccountIsCalled() {
        when(accountRepository.findById(ACCOUNT_NUMBER)).thenReturn(Optional.of(testAccount));

        Account result = accountService.findOrCreateAccount(ACCOUNT_NUMBER);

        assertEquals(testAccount, result);
        verify(accountRepository).findById(ACCOUNT_NUMBER);
        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    @DisplayName("Should create new account when findOrCreateAccount is called with non-existing account number")
    void shouldCreateNewAccountWhenFindOrCreateAccountIsCalledWithNonExistingAccount() {
        when(accountRepository.findById(ACCOUNT_NUMBER)).thenReturn(Optional.empty());
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Account result = accountService.findOrCreateAccount(ACCOUNT_NUMBER);

        assertEquals(ACCOUNT_NUMBER, result.accountNumber());
        assertEquals(BigDecimal.ZERO, result.balance());
        assertNotNull(result.createdDate());
        verify(accountRepository).findById(ACCOUNT_NUMBER);
        verify(accountRepository).save(any(Account.class));
    }

    @Test
    @DisplayName("Should update account when updateAccount is called")
    void shouldUpdateAccountWhenUpdateAccountIsCalled() {
        when(accountRepository.save(testAccount)).thenReturn(testAccount);

        Account result = accountService.updateAccount(testAccount);

        assertEquals(testAccount, result);
        verify(accountRepository).save(testAccount);
    }

    @Test
    @DisplayName("Should increase balance for deposit transaction when updateAccountBalance is called")
    void shouldIncreaseBalanceForDepositTransactionWhenUpdateAccountBalanceIsCalled() {
        BigDecimal depositAmount = BigDecimal.valueOf(500.00);
        Transaction depositTransaction = new Transaction(
                "TX123",
                LocalDate.now(),
                ACCOUNT_NUMBER,
                Transaction.TransactionType.DEPOSIT,
                depositAmount,
                BigDecimal.ZERO
        );

        when(accountRepository.findById(ACCOUNT_NUMBER)).thenReturn(Optional.of(testAccount));
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Account result = accountService.updateAccountBalance(depositTransaction);

        assertEquals(ACCOUNT_NUMBER, result.accountNumber());
        assertEquals(0, INITIAL_BALANCE.add(depositAmount).compareTo(result.balance()));
        assertEquals(CREATED_DATE, result.createdDate());
        verify(accountRepository).save(accountCaptor.capture());
        assertEquals(0, INITIAL_BALANCE.add(depositAmount).compareTo(accountCaptor.getValue().balance()));
    }

    @Test
    @DisplayName("Should increase balance for interest transaction when updateAccountBalance is called")
    void shouldIncreaseBalanceForInterestTransactionWhenUpdateAccountBalanceIsCalled() {
        BigDecimal interestAmount = BigDecimal.valueOf(50);
        Transaction interestTransaction = new Transaction(
                "TX123",
                LocalDate.now(),
                ACCOUNT_NUMBER,
                Transaction.TransactionType.INTEREST,
                interestAmount,
                BigDecimal.ZERO
        );

        when(accountRepository.findById(ACCOUNT_NUMBER)).thenReturn(Optional.of(testAccount));
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Account result = accountService.updateAccountBalance(interestTransaction);

        assertEquals(ACCOUNT_NUMBER, result.accountNumber());
        assertEquals(0, INITIAL_BALANCE.add(interestAmount).compareTo(result.balance()));
        assertEquals(CREATED_DATE, result.createdDate());
        verify(accountRepository).save(accountCaptor.capture());
        assertEquals(0,INITIAL_BALANCE.add(interestAmount).compareTo(accountCaptor.getValue().balance()));
    }

    @Test
    @DisplayName("Should decrease balance for withdrawal transaction when updateAccountBalance is called")
    void shouldDecreaseBalanceForWithdrawalTransactionWhenUpdateAccountBalanceIsCalled() {
        BigDecimal withdrawalAmount = BigDecimal.valueOf(300);
        Transaction withdrawalTransaction = new Transaction(
                "TX123",
                LocalDate.now(),
                ACCOUNT_NUMBER,
                Transaction.TransactionType.WITHDRAWAL,
                withdrawalAmount,
                BigDecimal.ZERO
        );

        when(accountRepository.findById(ACCOUNT_NUMBER)).thenReturn(Optional.of(testAccount));
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Account result = accountService.updateAccountBalance(withdrawalTransaction);

        assertEquals(ACCOUNT_NUMBER, result.accountNumber());
        assertEquals(0,INITIAL_BALANCE.subtract(withdrawalAmount).compareTo(result.balance()));
        assertEquals(CREATED_DATE, result.createdDate());
        verify(accountRepository).save(accountCaptor.capture());
        assertEquals(0,INITIAL_BALANCE.subtract(withdrawalAmount).compareTo(accountCaptor.getValue().balance()));
    }

    @Test
    @DisplayName("Should throw exception for withdrawal transaction with insufficient funds")
    void shouldThrowExceptionForWithdrawalTransactionWithInsufficientFunds() {
        BigDecimal withdrawalAmount = BigDecimal.valueOf(1500);
        Transaction withdrawalTransaction = new Transaction(
                "TX123",
                LocalDate.now(),
                ACCOUNT_NUMBER,
                Transaction.TransactionType.WITHDRAWAL,
                withdrawalAmount,
                BigDecimal.ZERO
        );

        when(accountRepository.findById(ACCOUNT_NUMBER)).thenReturn(Optional.of(testAccount));

        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                accountService.updateAccountBalance(withdrawalTransaction));
        assertEquals("Insufficient funds", exception.getMessage());
        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    @DisplayName("Should return transaction count when getAccountTransactionCount is called")
    void shouldReturnTransactionCountWhenGetAccountTransactionCountIsCalled() {
        List<Transaction> transactions = List.of(
                new Transaction("TX1", LocalDate.now(), ACCOUNT_NUMBER, Transaction.TransactionType.DEPOSIT, BigDecimal.TEN, BigDecimal.TEN),
                new Transaction("TX2", LocalDate.now(), ACCOUNT_NUMBER, Transaction.TransactionType.WITHDRAWAL, BigDecimal.ONE, BigDecimal.TEN.subtract(BigDecimal.ONE))
        );
        when(transactionRepository.findByAccountNumber(ACCOUNT_NUMBER)).thenReturn(transactions);

        int count = accountService.getAccountTransactionCount(ACCOUNT_NUMBER);

        assertEquals(2, count);
        verify(transactionRepository).findByAccountNumber(ACCOUNT_NUMBER);
    }

    @Test
    @DisplayName("Should return zero transaction count for account with no transactions")
    void shouldReturnZeroTransactionCountForAccountWithNoTransactions() {
        when(transactionRepository.findByAccountNumber(ACCOUNT_NUMBER)).thenReturn(Collections.emptyList());

        int count = accountService.getAccountTransactionCount(ACCOUNT_NUMBER);

        assertEquals(0, count);
        verify(transactionRepository).findByAccountNumber(ACCOUNT_NUMBER);
    }

    @Test
    @DisplayName("Should return account balance when getAccountBalance is called with existing account")
    void shouldReturnAccountBalanceWhenGetAccountBalanceIsCalledWithExistingAccount() {
        when(accountRepository.findById(ACCOUNT_NUMBER)).thenReturn(Optional.of(testAccount));

        BigDecimal balance = accountService.getAccountBalance(ACCOUNT_NUMBER);

        assertEquals(INITIAL_BALANCE, balance);
        verify(accountRepository).findById(ACCOUNT_NUMBER);
    }

    @Test
    @DisplayName("Should return zero when getAccountBalance is called with non-existing account")
    void shouldReturnZeroWhenGetAccountBalanceIsCalledWithNonExistingAccount() {
        when(accountRepository.findById(ACCOUNT_NUMBER)).thenReturn(Optional.empty());

        BigDecimal balance = accountService.getAccountBalance(ACCOUNT_NUMBER);

        assertEquals(BigDecimal.ZERO, balance);
        verify(accountRepository).findById(ACCOUNT_NUMBER);
    }
}
