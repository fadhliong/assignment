package org.awesomegic.service;

import org.awesomegic.model.Account;
import org.awesomegic.model.InterestRule;
import org.awesomegic.model.Transaction;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class StatementService {
    private final TransactionService transactionService;
    private final InterestRuleService interestRuleService;
    private final AccountService accountService;

    public StatementService(
            TransactionService transactionService,
            InterestRuleService interestRuleService,
            AccountService accountService) {
        this.transactionService = transactionService;
        this.interestRuleService = interestRuleService;
        this.accountService = accountService;
    }

    public void validateStatementInput(int year, int month) {
        LocalDate curr = LocalDate.now();
        LocalDate statementDate;

        try {
            statementDate = LocalDate.of(year, month, 1);
        } catch (DateTimeException e) {
            throw new IllegalArgumentException("Invalid year or month provided", e);
        }

        if(statementDate.isAfter(curr)) {
            throw new IllegalArgumentException("Year and month cannot be in the future");
        }
    }

    public List<Transaction> generateAccountStatement(
            String accountNumber, int year, int month) {

        YearMonth ym = YearMonth.of(year, month);

        LocalDate startDate = ym.atDay(1);
        LocalDate endDate = ym.atEndOfMonth();

        List<Transaction> existingTransactions = transactionService
                .findByAccountNumberAndDateRange(accountNumber, startDate, endDate);

        Transaction existingInterestTransaction = null;


        for (Transaction transaction : existingTransactions) {
            if (transaction.type() == Transaction.TransactionType.INTEREST) {
                existingInterestTransaction = transaction;

                transactionService.deleteById(transaction.id());
                break;
            }
        }

        Account account = accountService.findOrCreateAccount(accountNumber);
        BigDecimal endOfMonthBalance = calculateHistoricalBalance(accountNumber, endDate);

        List<Transaction> monthlyTransactions = transactionService
                .findByAccountNumberAndDateRange(accountNumber, startDate, endDate)
                .stream()
                .filter(t -> t.type() != Transaction.TransactionType.INTEREST)
                .sorted(Comparator.comparing(Transaction::date).thenComparing(Transaction::id))
                .collect(Collectors.toList());

        Transaction interestTransaction = calculateMonthlyInterest(
                accountNumber,
                startDate,
                endDate
        );

        if (interestTransaction != null) {
            BigDecimal interestAdjustedBalance = endOfMonthBalance.add(interestTransaction.amount());

            if (existingInterestTransaction != null) {
                interestTransaction = new Transaction(
                        existingInterestTransaction.id(),
                        interestTransaction.date(),
                        interestTransaction.accountNumber(),
                        interestTransaction.type(),
                        interestTransaction.amount(),
                        interestAdjustedBalance
                );
            } else {
                interestTransaction = new Transaction(
                        interestTransaction.id(),
                        interestTransaction.date(),
                        interestTransaction.accountNumber(),
                        interestTransaction.type(),
                        interestTransaction.amount(),
                        interestAdjustedBalance
                );
            }

            Transaction savedInterestTransaction = transactionService.save(interestTransaction);

            if (YearMonth.from(LocalDate.now()).equals(YearMonth.of(year, month))) {
                Account updatedAccount = new Account(
                        account.accountNumber(),
                        interestAdjustedBalance,
                        account.createdDate()
                );
                accountService.updateAccount(updatedAccount);
            }

            monthlyTransactions.add(savedInterestTransaction);
        }

        monthlyTransactions.sort(Comparator.comparing(Transaction::date));

        return monthlyTransactions;
    }

    private Transaction calculateMonthlyInterest(
            String accountNumber,
            LocalDate startDate,
            LocalDate endDate) {

        List<InterestRulePeriod> interestRulePeriods = findInterestRulePeriods(startDate, endDate);

        if (interestRulePeriods.isEmpty()) {
            return null;
        }

        BigDecimal totalInterest = calculateTotalInterest(
                accountNumber,
                interestRulePeriods
        );

        if (totalInterest.compareTo(BigDecimal.ZERO) > 0) {
            return createInterestTransaction(accountNumber, endDate, totalInterest);
        }

        return null;
    }

    private List<InterestRulePeriod> findInterestRulePeriods(
            LocalDate startDate,
            LocalDate endDate) {
        List<InterestRule> allRules = interestRuleService.getAllInterestRules()
                .stream()
                .sorted(Comparator.comparing(InterestRule::effectiveDate))
                .collect(Collectors.toList());

        if (allRules.isEmpty()) {
            return new ArrayList<>();
        }

        List<InterestRulePeriod> periods = new ArrayList<>();

        Optional<InterestRule> startingRuleOpt = allRules.stream()
                .filter(rule -> !rule.effectiveDate().isAfter(startDate))
                .max(Comparator.comparing(InterestRule::effectiveDate));

        if (startingRuleOpt.isEmpty()) {
            return periods;
        }

        InterestRule currentRule = startingRuleOpt.get();
        LocalDate currentPeriodStart = startDate;

        List<InterestRule> rulesWithinPeriod = allRules.stream()
                .filter(rule -> rule.effectiveDate().isAfter(startDate) && !rule.effectiveDate().isAfter(endDate))
                .sorted(Comparator.comparing(InterestRule::effectiveDate))
                .collect(Collectors.toList());

        for (InterestRule rule : rulesWithinPeriod) {
            periods.add(new InterestRulePeriod(
                    currentPeriodStart,
                    rule.effectiveDate().minusDays(1),
                    currentRule
            ));

            currentPeriodStart = rule.effectiveDate();
            currentRule = rule;
        }

        periods.add(new InterestRulePeriod(
                currentPeriodStart,
                endDate,
                currentRule
        ));

        return periods;
    }

    private BigDecimal calculateTotalInterest(
            String accountNumber,
            List<InterestRulePeriod> interestRulePeriods) {

        BigDecimal totalInterest = BigDecimal.ZERO;

        LocalDate startDate = interestRulePeriods.get(0).startDate();
        LocalDate endDate = interestRulePeriods.get(interestRulePeriods.size() - 1).endDate();

        List<DailyBalance> dailyBalances = calculateDailyBalances(accountNumber, startDate, endDate);

        for (InterestRulePeriod period : interestRulePeriods) {
            BigDecimal periodInterest = BigDecimal.ZERO;

            List<DailyBalance> periodBalances = dailyBalances.stream()
                    .filter(db ->
                            !db.date().isBefore(period.startDate()) &&
                                    !db.date().isAfter(period.endDate()))
                    .collect(Collectors.toList());

            for (DailyBalance dailyBalance : periodBalances) {
                BigDecimal dailyInterest = dailyBalance.balance()
                        .multiply(period.interestRule().interestRate())
                        .divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP)
                        .divide(BigDecimal.valueOf(365), 10, RoundingMode.HALF_UP);

                periodInterest = periodInterest.add(dailyInterest);
            }

            totalInterest = totalInterest.add(periodInterest);
        }

        return totalInterest.setScale(2, RoundingMode.HALF_UP);
    }

    private List<DailyBalance> calculateDailyBalances(
            String accountNumber,
            LocalDate startDate,
            LocalDate endDate) {

        List<Transaction> allTransactions = transactionService
                .findByAccountNumberAndDateRange(accountNumber, LocalDate.MIN, endDate)
                .stream()
                .filter(t -> t.type() != Transaction.TransactionType.INTEREST)
                .sorted(Comparator.comparing(Transaction::date))
                .collect(Collectors.toList());

        List<DailyBalance> dailyBalances = new ArrayList<>();
        BigDecimal runningBalance = BigDecimal.ZERO;

        for (Transaction txn : allTransactions) {
            if (txn.date().isBefore(startDate)) {
                runningBalance = updateBalance(runningBalance, txn);
            }
        }

        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            final LocalDate currentDate = date;

            List<Transaction> transactionsForDay = allTransactions.stream()
                    .filter(t -> t.date().equals(currentDate))
                    .collect(Collectors.toList());

            for (Transaction txn : transactionsForDay) {
                runningBalance = updateBalance(runningBalance, txn);
            }

            dailyBalances.add(new DailyBalance(currentDate, runningBalance));
        }

        return dailyBalances;
    }

    private Transaction createInterestTransaction(
            String accountNumber,
            LocalDate valueDate,
            BigDecimal interestAmount) {
        return new Transaction(
                "ID",
                valueDate,
                accountNumber,
                Transaction.TransactionType.INTEREST,
                interestAmount,
                BigDecimal.ZERO
        );
    }

    private BigDecimal updateBalance(BigDecimal currentBalance, Transaction transaction) {
        return switch (transaction.type()) {
            case DEPOSIT, INTEREST -> currentBalance.add(transaction.amount());
            case WITHDRAWAL -> currentBalance.subtract(transaction.amount());
        };
    }

    private BigDecimal calculateHistoricalBalance(String accountNumber, LocalDate asOfDate) {
        List<Transaction> allTransactions = transactionService
                .findByAccountNumberAndDateRange(accountNumber, LocalDate.MIN, asOfDate)
                .stream()
                .sorted(Comparator.comparing(Transaction::date).thenComparing(Transaction::id))
                .collect(Collectors.toList());

        if (allTransactions.isEmpty()) {
            return BigDecimal.ZERO;
        }

        Transaction lastTransaction = allTransactions.get(allTransactions.size() - 1);

        if (lastTransaction.date().equals(asOfDate)) {
            return lastTransaction.balance();
        }

        BigDecimal runningBalance = BigDecimal.ZERO;

        for (Transaction transaction : allTransactions) {
            runningBalance = switch (transaction.type()) {
                case DEPOSIT, INTEREST -> runningBalance.add(transaction.amount());
                case WITHDRAWAL -> runningBalance.subtract(transaction.amount());
            };
        }

        return runningBalance;
    }

    private record InterestRulePeriod(
            LocalDate startDate,
            LocalDate endDate,
            InterestRule interestRule
    ) {}

    private record DailyBalance(
            LocalDate date,
            BigDecimal balance
    ) {}
}
