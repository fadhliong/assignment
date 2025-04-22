package org.awesomegic.menu;

import org.awesomegic.config.BankingConfiguration;
import org.awesomegic.model.InterestRule;
import org.awesomegic.model.Transaction;
import org.awesomegic.service.AccountService;
import org.awesomegic.service.InterestRuleService;
import org.awesomegic.service.StatementService;
import org.awesomegic.service.TransactionService;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Scanner;

public class BankingMenu {
    private final Scanner scanner;
    private final AccountService accountService;
    private final TransactionService transactionService;
    private final InterestRuleService interestRuleService;
    private final StatementService statementService;


    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMdd");

    public BankingMenu(Scanner scanner,
                       AccountService accountService,
                       TransactionService transactionService,
                       InterestRuleService interestRuleService,
                       StatementService statementService) {
        this.scanner = scanner;
        this.accountService = accountService;
        this.transactionService = transactionService;
        this.interestRuleService = interestRuleService;
        this.statementService = statementService;
    }


    public void start() {
        displayWelcomeMessage();

        boolean running = true;
        boolean firstRun = true;
        while(running) {
            if(!firstRun) {
                System.out.println("\nIs there anything else you'd like to do?");
            }
            displayMainMenu();
            String choice = scanner.nextLine().trim().toUpperCase();
            running = processMainMenuChoice(choice);
            firstRun = false;
        }
    }

    private void displayWelcomeMessage() {
        System.out.println("Welcome to AwesomeGIC Bank!");
        System.out.println("What would you like to do?");
    }

    private void displayMainMenu() {
        System.out.println("[T] Input transactions");
        System.out.println("[I] Define interest rules");
        System.out.println("[P] Print statement");
        System.out.println("[Q] Quit");
        System.out.print("> ");
    }

    private boolean processMainMenuChoice(String choice) {
        switch (choice) {
            case "T" -> handleTransactionInput();
            case "I" -> handleInterestRule();
            case "P" -> handleStatement();
            case "Q" -> {
                displayQuitMessage();
                return false;
            }
            default -> System.out.println("Invalid choice. Please try again.");
        }
        return true;
    }

    private void handleTransactionInput() {
        while (true) {
            System.out.println("\nPlease enter transaction details in <Date> <Account> <Type> <Amount> format");
            System.out.println("(or enter blank to go back to main menu):");
            System.out.print("> ");

            String input = scanner.nextLine().trim();

            if (input.isEmpty()) {
                return;
            }

            try {
                Transaction transaction = transactionService.processTransaction(input);
                displayTransactions(transaction.accountNumber());

                break;
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
    }

    private void handleInterestRule() {
        while (true) {
            System.out.println("\nPlease enter interest rules details in <Date> <RuleId> <Rate in %> format");
            System.out.println("(or enter blank to go back to main menu):");
            System.out.print("> ");

            String input = scanner.nextLine().trim();

            if (input.isEmpty()) {
                return;
            }

            try {
                interestRuleService.processInterestRule(input);
                displayInterestRules();
                break;
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
    }

    private void handleStatement() {
        while (true) {
            System.out.println("\nPlease enter account and month to generate the statement <Account> <Year><Month>");
            System.out.println("(or enter blank to go back to main menu):");
            System.out.print("> ");

            String input = scanner.nextLine().trim();

            if (input.isEmpty()) {
                return;
            }

            try {
                String[] parts = input.split("\\s+");
                if (parts.length != 2) {
                    System.out.println("Invalid input. Please use <Account> <Year><Month> format.");
                    continue;
                }

                String accountNumber = parts[0];
                int year = Integer.parseInt(parts[1].substring(0, 4));
                int month = Integer.parseInt(parts[1].substring(4));

                statementService.validateStatementInput(year,month);

                displayAccountStatement(accountNumber, year, month);

                break;
            } catch (NumberFormatException e) {
                System.out.println("Invalid year/month format. Please use YYYYMM format.");
            } catch (Exception e) {
                System.out.println("Error generating statement: " + e.getMessage());
            }
        }
    }

    private void displayTransactions(String accountNumber) {
        List<Transaction> transactions = transactionService.getTransactionsByAccountNumber(accountNumber);
        System.out.println("\nAccount: " + accountNumber);
        System.out.println("| Date | Txn Id | Type | Amount |");
        transactions.forEach(txn ->
                System.out.printf("| %s | %s | %s | %.2f |\n",
                        txn.date(), txn.id(), txn.type(), txn.amount())
        );
    }

    private void displayAccountStatement(String accountNumber, int year, int month) {
        List<Transaction> transactions = statementService.generateAccountStatement(accountNumber, year, month);
        //List<Transaction> transactions = statementService.generateAccountStatement(accountNumber);
        System.out.println("\nAccount: " + accountNumber);
        System.out.println("| Date | Txn Id | Type | Amount | Balance |");
        transactions.forEach(txn ->
                System.out.printf("| %s | %s | %s | %.2f | %.2f \n",
                        txn.date(), txn.id(), txn.type(), txn.amount(), txn.balance())
        );

        BigDecimal balance = accountService.getAccountBalance(accountNumber);
        System.out.printf("\nCurrent Balance: %.2f\n", balance);
    }

    private void displayInterestRules() {
        List<InterestRule> rules = interestRuleService.getAllInterestRules();

        System.out.println("\nInterest rules:");
        System.out.println("| Date | RuleId | Rate (%) |");
        rules.forEach(rule ->
                System.out.printf("| %s | %s | %.2f |\n",
                        rule.effectiveDate(), rule.ruleId(), rule.interestRate())
        );
    }

    public void displayQuitMessage() {
        System.out.println("\nThank you for banking with AwesomeGIC Bank.");
        System.out.println("Have a nice day!");
    }
}
