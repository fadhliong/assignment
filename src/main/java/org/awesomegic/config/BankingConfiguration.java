package org.awesomegic.config;

import org.awesomegic.model.Account;
import org.awesomegic.model.InterestRule;
import org.awesomegic.model.Transaction;
import org.awesomegic.repositoy.*;
import org.awesomegic.service.AccountService;
import org.awesomegic.service.InterestRuleService;
import org.awesomegic.service.StatementService;
import org.awesomegic.service.TransactionService;

public class BankingConfiguration {

    private static BankingConfiguration instance;

    private final TransactionRepository transactionRepository;
    private final InterestRuleRepository interestRuleRepository;
    private final AccountRepository accountRepository;

    private final AccountService accountService;
    private final TransactionService transactionService;
    private final InterestRuleService interestRuleService;
    private final StatementService statementService;

    private BankingConfiguration() {
        this.transactionRepository = new InMemoryTransactionRepository();
        this.interestRuleRepository = new InMemoryInterestRuleRepository();
        this.accountRepository = new InMemoryAccountRepository();

        this.accountService = new AccountService(transactionRepository,accountRepository);
        this.transactionService = new TransactionService(transactionRepository, accountService);
        this.interestRuleService = new InterestRuleService(interestRuleRepository);
        this.statementService = new StatementService(
                transactionService,
                interestRuleService,
                accountService);
    }

    public static synchronized BankingConfiguration getInstance() {
        if (instance == null) {
            instance = new BankingConfiguration();
        }
        return instance;
    }

    public TransactionRepository getTransactionRepository() {
        return transactionRepository;
    }

    public InterestRuleRepository getInterestRuleRepository() {
        return interestRuleRepository;
    }

    public AccountRepository getAccountRepository() {
        return accountRepository;
    }

    public AccountService getAccountService() {
        return accountService;
    }

    public TransactionService getTransactionService() {
        return transactionService;
    }

    public InterestRuleService getInterestRuleService() {
        return interestRuleService;
    }

    public StatementService getStatementService() {
        return statementService;
    }
}