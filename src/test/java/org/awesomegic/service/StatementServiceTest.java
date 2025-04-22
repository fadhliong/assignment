package org.awesomegic.service;

import org.awesomegic.model.Account;
import org.awesomegic.model.InterestRule;
import org.awesomegic.model.Transaction;
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
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class StatementServiceTest {
    @Mock
    private TransactionService transactionService;

    @Mock
    private InterestRuleService interestRuleService;

    @Mock
    private AccountService accountService;

    @InjectMocks
    private StatementService statementService;

    @Captor
    private ArgumentCaptor<Transaction> transactionCaptor;

    private final String ACCOUNT_NUMBER = "ACC123";
    private final LocalDate STATEMENT_DATE = LocalDate.of(2025, 3, 1);
    private final int STATEMENT_YEAR = 2025;
    private final int STATEMENT_MONTH = 3;
    private Account testAccount;
    private List<Transaction> testTransactions;
    private List<InterestRule> testInterestRules;

    @BeforeEach
    void setUp() {
        testAccount = new Account(ACCOUNT_NUMBER, BigDecimal.valueOf(1000.00), LocalDate.of(2025, 1, 1));

        testTransactions = new ArrayList<>();
        testTransactions.add(new Transaction(
                "20250101-01",
                LocalDate.of(2025, 1, 1),
                ACCOUNT_NUMBER,
                Transaction.TransactionType.DEPOSIT,
                BigDecimal.valueOf(1000.00),
                BigDecimal.valueOf(1000.00)
        ));
        testTransactions.add(new Transaction(
                "20250215-01",
                LocalDate.of(2025, 2, 15),
                ACCOUNT_NUMBER,
                Transaction.TransactionType.DEPOSIT,
                BigDecimal.valueOf(500.00),
                BigDecimal.valueOf(1500.00)
        ));
        testTransactions.add(new Transaction(
                "20250310-01",
                LocalDate.of(2025, 3, 10),
                ACCOUNT_NUMBER,
                Transaction.TransactionType.WITHDRAWAL,
                BigDecimal.valueOf(200.00),
                BigDecimal.valueOf(1300.00)
        ));

        testInterestRules = new ArrayList<>();
        testInterestRules.add(new InterestRule(
                LocalDate.of(2025, 1, 1),
                "RULE-001",
                BigDecimal.valueOf(2.5)
        ));
        testInterestRules.add(new InterestRule(
                LocalDate.of(2025, 2, 1),
                "RULE-002",
                BigDecimal.valueOf(3.0)
        ));
    }

    @Test
    @DisplayName("Should validate valid statement input")
    void shouldValidateValidStatementInput() {
        int validYear = 2025;
        int validMonth = 3;

        assertDoesNotThrow(() -> statementService.validateStatementInput(validYear, validMonth));
    }

    @Test
    @DisplayName("Should throw exception when statement month is invalid")
    void shouldThrowExceptionWhenStatementMonthIsInvalid() {
        int validYear = 2025;
        int invalidMonth = 13;

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                statementService.validateStatementInput(validYear, invalidMonth));
        assertTrue(exception.getMessage().contains("Invalid year or month provided"));
    }

    @Test
    @DisplayName("Should throw exception when statement date is in the future")
    void shouldThrowExceptionWhenStatementDateIsInTheFuture() {
        int futureYear = LocalDate.now().getYear() + 1;
        int futureMonth = 1;

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                statementService.validateStatementInput(futureYear, futureMonth));
        assertEquals("Year and month cannot be in the future", exception.getMessage());
    }

    @Test
    @DisplayName("Should generate account statement with existing transactions")
    void shouldGenerateAccountStatementWithExistingTransactions() {
        YearMonth statementYearMonth = YearMonth.of(2025, 3);
        LocalDate startDate = statementYearMonth.atDay(1);
        LocalDate endDate = statementYearMonth.atEndOfMonth();

        List<Transaction> marchTransactions = List.of(
                new Transaction("20250310-01", LocalDate.of(2025, 3, 10), ACCOUNT_NUMBER,
                        Transaction.TransactionType.WITHDRAWAL, BigDecimal.valueOf(200.00), BigDecimal.valueOf(1300.00))
        );

        when(accountService.findOrCreateAccount(ACCOUNT_NUMBER)).thenReturn(testAccount);
        when(transactionService.findByAccountNumberAndDateRange(ACCOUNT_NUMBER, startDate, endDate))
                .thenReturn(marchTransactions);
        when(transactionService.findByAccountNumberAndDateRange(eq(ACCOUNT_NUMBER), eq(LocalDate.MIN), eq(endDate)))
                .thenReturn(testTransactions);
        when(interestRuleService.getAllInterestRules()).thenReturn(testInterestRules);

        when(transactionService.save(any(Transaction.class)))
                .thenAnswer(invocation -> {
                    Transaction t = invocation.getArgument(0);
                    if (t.id().isEmpty()) {
                        return new Transaction("20250331-01", t.date(), t.accountNumber(),
                                t.type(), t.amount(), t.balance());
                    }
                    return t;
                });

        List<Transaction> result = statementService.generateAccountStatement(ACCOUNT_NUMBER, 2025, 3);

        assertEquals(2, result.size());

        Transaction interestTransaction = result.stream()
                .filter(t -> t.type() == Transaction.TransactionType.INTEREST)
                .findFirst()
                .orElseThrow();

        assertEquals(LocalDate.of(2025, 3, 31), interestTransaction.date());
        assertTrue(interestTransaction.amount().compareTo(BigDecimal.ZERO) > 0);

        verify(transactionService).save(any(Transaction.class));
    }

    @Test
    @DisplayName("Should handle existing interest transaction when generating statement")
    void shouldHandleExistingInterestTransactionWhenGeneratingStatement() {
        YearMonth statementYearMonth = YearMonth.of(2025, 3);
        LocalDate startDate = statementYearMonth.atDay(1);
        LocalDate endDate = statementYearMonth.atEndOfMonth();

        Transaction existingInterestTransaction = new Transaction(
                "20250331-01",
                LocalDate.of(2025, 3, 31),
                ACCOUNT_NUMBER,
                Transaction.TransactionType.INTEREST,
                BigDecimal.valueOf(9.00),
                BigDecimal.valueOf(1309.00)
        );

        List<Transaction> marchTransactions = new ArrayList<>();
        marchTransactions.add(new Transaction(
                "20250310-01",
                LocalDate.of(2025, 3, 10),
                ACCOUNT_NUMBER,
                Transaction.TransactionType.WITHDRAWAL,
                BigDecimal.valueOf(200.00),
                BigDecimal.valueOf(1300.00)
        ));
        marchTransactions.add(existingInterestTransaction);

        when(accountService.findOrCreateAccount(ACCOUNT_NUMBER)).thenReturn(testAccount);
        when(transactionService.findByAccountNumberAndDateRange(ACCOUNT_NUMBER, startDate, endDate))
                .thenReturn(marchTransactions);

        when(transactionService.findByAccountNumberAndDateRange(ACCOUNT_NUMBER, startDate, endDate))
                .thenReturn(marchTransactions)
                .thenReturn(List.of(marchTransactions.get(0))); // Second call should return without interest transaction

        when(transactionService.findByAccountNumberAndDateRange(eq(ACCOUNT_NUMBER), eq(LocalDate.MIN), eq(endDate)))
                .thenReturn(testTransactions);
        when(interestRuleService.getAllInterestRules()).thenReturn(testInterestRules);

        when(transactionService.save(any(Transaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        List<Transaction> result = statementService.generateAccountStatement(ACCOUNT_NUMBER, 2025, 3);

        verify(transactionService).deleteById(existingInterestTransaction.id());
        verify(transactionService).save(transactionCaptor.capture());

        Transaction newInterestTransaction = transactionCaptor.getValue();
        assertEquals(existingInterestTransaction.id(), newInterestTransaction.id());
        assertTrue(newInterestTransaction.amount().compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    @DisplayName("Should not add interest transaction when no interest rules are applicable")
    void shouldNotAddInterestTransactionWhenNoInterestRulesAreApplicable() {
        YearMonth statementYearMonth = YearMonth.of(2025, 3);
        LocalDate startDate = statementYearMonth.atDay(1);
        LocalDate endDate = statementYearMonth.atEndOfMonth();

        List<Transaction> marchTransactions = List.of(
                new Transaction("20250310-01", LocalDate.of(2025, 3, 10), ACCOUNT_NUMBER,
                        Transaction.TransactionType.WITHDRAWAL, BigDecimal.valueOf(200.00), BigDecimal.valueOf(1300.00))
        );

        when(accountService.findOrCreateAccount(ACCOUNT_NUMBER)).thenReturn(testAccount);
        when(transactionService.findByAccountNumberAndDateRange(ACCOUNT_NUMBER, startDate, endDate))
                .thenReturn(marchTransactions);
        when(interestRuleService.getAllInterestRules()).thenReturn(new ArrayList<>());

        when(transactionService.findByAccountNumberAndDateRange(eq(ACCOUNT_NUMBER), eq(LocalDate.MIN), eq(endDate)))
                .thenReturn(marchTransactions);

        List<Transaction> result = statementService.generateAccountStatement(ACCOUNT_NUMBER, 2025, 3);

        assertEquals(1, result.size()); // Only the existing withdrawal transaction
        verify(transactionService, never()).save(any(Transaction.class));
        verify(accountService, never()).updateAccount(any(Account.class));
    }

    @Test
    @DisplayName("Should update account balance when generating statement for current month")
    void shouldUpdateAccountBalanceWhenGeneratingStatementForCurrentMonth() {
        int currentYear = LocalDate.now().getYear();
        int currentMonth = LocalDate.now().getMonthValue();
        YearMonth statementYearMonth = YearMonth.of(currentYear, currentMonth);
        LocalDate startDate = statementYearMonth.atDay(1);
        LocalDate endDate = statementYearMonth.atEndOfMonth();

        List<Transaction> currentMonthTransactions = new ArrayList<>();

        when(accountService.findOrCreateAccount(ACCOUNT_NUMBER)).thenReturn(testAccount);
        when(transactionService.findByAccountNumberAndDateRange(ACCOUNT_NUMBER, startDate, endDate))
                .thenReturn(currentMonthTransactions);
        when(transactionService.findByAccountNumberAndDateRange(eq(ACCOUNT_NUMBER), eq(LocalDate.MIN), eq(endDate)))
                .thenReturn(testTransactions);
        when(interestRuleService.getAllInterestRules()).thenReturn(testInterestRules);

        when(transactionService.save(any(Transaction.class)))
                .thenAnswer(invocation -> {
                    Transaction t = invocation.getArgument(0);
                    if (t.id().isEmpty()) {
                        return new Transaction(
                                t.date().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd")) + "-01",
                                t.date(), t.accountNumber(), t.type(), t.amount(), t.balance());
                    }
                    return t;
                });

        List<Transaction> result = statementService.generateAccountStatement(ACCOUNT_NUMBER, currentYear, currentMonth);

        verify(accountService).updateAccount(any(Account.class));
    }

    @Test
    @DisplayName("Should handle account with no transactions")
    void shouldHandleAccountWithNoTransactions() {
        YearMonth statementYearMonth = YearMonth.of(2025, 3);
        LocalDate startDate = statementYearMonth.atDay(1);
        LocalDate endDate = statementYearMonth.atEndOfMonth();

        when(accountService.findOrCreateAccount(ACCOUNT_NUMBER)).thenReturn(testAccount);
        when(transactionService.findByAccountNumberAndDateRange(ACCOUNT_NUMBER, startDate, endDate))
                .thenReturn(new ArrayList<>());
        when(transactionService.findByAccountNumberAndDateRange(eq(ACCOUNT_NUMBER), eq(LocalDate.MIN), eq(endDate)))
                .thenReturn(new ArrayList<>());
        when(interestRuleService.getAllInterestRules()).thenReturn(testInterestRules);

        List<Transaction> result = statementService.generateAccountStatement(ACCOUNT_NUMBER, 2025, 3);

        assertTrue(result.isEmpty());
        verify(transactionService, never()).save(any(Transaction.class));
    }

    @Test
    @DisplayName("Should handle multiple interest rule changes within statement period")
    void shouldHandleMultipleInterestRuleChangesWithinStatementPeriod() {
        YearMonth statementYearMonth = YearMonth.of(2025, 3);
        LocalDate startDate = statementYearMonth.atDay(1);
        LocalDate endDate = statementYearMonth.atEndOfMonth();

        testInterestRules.add(new InterestRule(
                LocalDate.of(2025, 3, 15),
                "RULE-003",
                BigDecimal.valueOf(3.5)
        ));

        List<Transaction> marchTransactions = List.of(
                new Transaction("20250310-01", LocalDate.of(2025, 3, 10), ACCOUNT_NUMBER,
                        Transaction.TransactionType.WITHDRAWAL, BigDecimal.valueOf(200.00), BigDecimal.valueOf(1300.00))
        );

        when(accountService.findOrCreateAccount(ACCOUNT_NUMBER)).thenReturn(testAccount);
        when(transactionService.findByAccountNumberAndDateRange(ACCOUNT_NUMBER, startDate, endDate))
                .thenReturn(marchTransactions);
        when(transactionService.findByAccountNumberAndDateRange(eq(ACCOUNT_NUMBER), eq(LocalDate.MIN), eq(endDate)))
                .thenReturn(testTransactions);
        when(interestRuleService.getAllInterestRules()).thenReturn(testInterestRules);

        when(transactionService.save(any(Transaction.class)))
                .thenAnswer(invocation -> {
                    Transaction t = invocation.getArgument(0);
                    if (t.id().isEmpty()) {
                        return new Transaction("20250331-01", t.date(), t.accountNumber(),
                                t.type(), t.amount(), t.balance());
                    }
                    return t;
                });

        List<Transaction> result = statementService.generateAccountStatement(ACCOUNT_NUMBER, 2025, 3);

        assertEquals(2, result.size());

        Transaction interestTransaction = result.stream()
                .filter(t -> t.type() == Transaction.TransactionType.INTEREST)
                .findFirst()
                .orElseThrow();

        assertEquals(LocalDate.of(2025, 3, 31), interestTransaction.date());
        assertTrue(interestTransaction.amount().compareTo(BigDecimal.ZERO) > 0);

        verify(transactionService).save(any(Transaction.class));
    }
}