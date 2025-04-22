package org.awesomegic.menu;

import org.awesomegic.model.InterestRule;
import org.awesomegic.model.Transaction;
import org.awesomegic.service.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BankingMenuTest {

    @Mock
    private AccountService accountService;

    @Mock
    private TransactionService transactionService;

    @Mock
    private InterestRuleService interestRuleService;

    @Mock
    private StatementService statementService;

    @InjectMocks
    private BankingMenu bankingMenu;
    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;

    @BeforeEach
    void setup() {
        System.setOut(new PrintStream(outContent));
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
    }

    private void setupBankingMenuWithInput(String input) {
        Scanner scanner = new Scanner(new ByteArrayInputStream(input.getBytes()));
        bankingMenu = new BankingMenu(
                scanner,
                accountService,
                transactionService,
                interestRuleService,
                statementService
        );
    }

    @Test
    @DisplayName("should display welcome message and menu")
    void start_shouldDisplayWelcomeMessageAndMenu() {
        setupBankingMenuWithInput("Q");

        bankingMenu.start();

        String output = outContent.toString();
        assertTrue(output.contains("Welcome to AwesomeGIC Bank"));
        assertTrue(output.contains("[T] Input transactions"));
        assertTrue(output.contains("[I] Define interest rules"));
        assertTrue(output.contains("[P] Print statement"));
        assertTrue(output.contains("[Q] Quit"));
        assertTrue(output.contains("Thank you for banking with AwesomeGIC Bank"));
    }

    @Test
    @DisplayName("should accept invalid option then q option")
    void start_shouldAcceptInvalidOptionThenQOption() {
        setupBankingMenuWithInput("X\nQ\n");

        bankingMenu.start();

        String output = outContent.toString();
        assertTrue(output.contains("Invalid choice"));
        assertTrue(output.contains("Is there anything else you'd like to do?"));
        assertTrue(output.contains("Thank you for banking with AwesomeGIC Bank"));
    }

    @Test
    @DisplayName("should accept invalid option then p option")
    void start_shouldAcceptInvalidOptionThenPrintOption() {
        setupBankingMenuWithInput("X\nP\n\s\nQ\n");

        bankingMenu.start();

        String output = outContent.toString();
        assertTrue(output.contains("Invalid choice"));
        assertTrue(output.contains("Is there anything else you'd like to do?"));
        assertTrue(output.contains("Please enter account and month to generate the statement <Account> <Year><Month>"));
        assertTrue(output.contains("Thank you for banking with AwesomeGIC Bank"));
    }

    @Test
    @DisplayName("should process valid transaction")
    void handleTransactionInput_shouldProcessValidTransaction() {
        setupBankingMenuWithInput("T\n20250101 ACC123 D 100.00\nQ\n");
        Transaction mockTransaction = new Transaction("ID", LocalDate.of(2025, 1, 1),  "ACC123", Transaction.TransactionType.DEPOSIT, new BigDecimal("100.00"), new BigDecimal("100.00"));
        when(transactionService.processTransaction(anyString())).thenReturn(mockTransaction);
        when(transactionService.getTransactionsByAccountNumber(anyString())).thenReturn(List.of(mockTransaction));

        bankingMenu.start();
        String output = outContent.toString();
        assertTrue(output.contains("Please enter transaction details in <Date> <Account> <Type> <Amount> format"));
        verify(transactionService).processTransaction("20250101 ACC123 D 100.00");
        assertTrue(output.contains("Account: ACC123"));
        assertTrue(output.contains("| 2025-01-01 | ID | D | 100.00 |"));
    }

    @Test
    @DisplayName("should handle exception for transaction")
    void handleTransactionInput_shouldHandleExceptionForTransaction() {
        setupBankingMenuWithInput("T\n20250101 ACC123 DEPOSIT 100.00\n\s\nQ\n");
        when(transactionService.processTransaction(anyString())).thenThrow(new RuntimeException("Invalid transaction format"));

        bankingMenu.start();

        String output = outContent.toString();

        verify(transactionService).processTransaction("20250101 ACC123 DEPOSIT 100.00");
        assertTrue(output.contains("Please enter transaction details in <Date> <Account> <Type> <Amount> format"));

        assertTrue(output.contains("Error: Invalid transaction format"));
        assertFalse(output.contains("Account: ACC123"));
        assertFalse(output.contains("| 2025-01-01 | ID | D | 100.00 |"));
    }

    @Test
    @DisplayName("should handle process interest rule")
    void handleInterestRules_shouldProcessInterestRule() {
        setupBankingMenuWithInput("I\n20250101 RULE1 10\nQ\n");
        List<InterestRule> mockRules = List.of(
                new InterestRule(LocalDate.of(2025, 1, 1),"RULE1", new BigDecimal(10)),
                new InterestRule(LocalDate.of(2025, 6, 1),"RULE2", new BigDecimal(4))
        );

        when(interestRuleService.getAllInterestRules()).thenReturn(mockRules);

        bankingMenu.start();

        String output = outContent.toString();

        assertTrue(output.contains("Please enter interest rules details in <Date> <RuleId> <Rate in %> format"));
        assertTrue(output.contains("Interest rules:"));
        assertTrue(output.contains("| Date | RuleId | Rate (%) |"));
        assertTrue(output.contains("| 2025-01-01 | RULE1 | 10.00 |"));
        assertTrue(output.contains("| 2025-06-01 | RULE2 | 4.00 |"));
    }

    @Test
    @DisplayName("should handle exception for interest rule")
    void handleInterestRules_shouldHandleExceptionInterestRule() {
        setupBankingMenuWithInput("I\n20250101 RULE1 10\n\s\nQ\n");
        List<InterestRule> mockRules = List.of(
                new InterestRule(LocalDate.of(2025, 1, 1),"RULE1", new BigDecimal(10)),
                new InterestRule(LocalDate.of(2025, 6, 1),"RULE2", new BigDecimal(4))
        );

        doThrow(new RuntimeException("Exception"))
                .when(interestRuleService)
                .processInterestRule(anyString());

        bankingMenu.start();

        String output = outContent.toString();

        assertTrue(output.contains("Please enter interest rules details in <Date> <RuleId> <Rate in %> format"));
        assertTrue(output.contains("Error: Exception"));
        assertFalse(output.contains("Interest rules:"));
    }

    @Test
    @DisplayName("should handle process statement")
    void handleStatement_shouldProcessStatement() {
        setupBankingMenuWithInput("P\nACC1 202501\nQ\n");
        List<Transaction> mockTransactions = List.of(
                new Transaction("id1", LocalDate.of(2025,1,1),"ACC1", Transaction.TransactionType.DEPOSIT,BigDecimal.valueOf(100), BigDecimal.valueOf(100)),
                new Transaction("id2", LocalDate.of(2025,1,1),"ACC1", Transaction.TransactionType.DEPOSIT,BigDecimal.valueOf(100), BigDecimal.valueOf(200))
                );

        when(statementService.generateAccountStatement(anyString(),anyInt(),anyInt())).thenReturn(mockTransactions);
        when(accountService.getAccountBalance(anyString())).thenReturn(BigDecimal.valueOf(200));

        bankingMenu.start();

        String output = outContent.toString();

        assertTrue(output.contains("Please enter account and month to generate the statement <Account> <Year><Month>"));
        assertTrue(output.contains("Account: ACC1"));
        assertTrue(output.contains("| Date | Txn Id | Type | Amount | Balance |"));
        assertTrue(output.contains("| 2025-01-01 | id1 | D | 100.00 | 100.00"));
        assertTrue(output.contains("| 2025-01-01 | id2 | D | 100.00 | 200.00"));
        assertTrue(output.contains("Current Balance: 200.00"));
    }
}