package org.awesomegic.service;

import org.awesomegic.model.Account;
import org.awesomegic.model.Transaction;
import org.awesomegic.repositoy.AccountRepository;
import org.awesomegic.repositoy.TransactionRepository;

import java.math.BigDecimal;
import java.util.Optional;

public class AccountService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;

    public AccountService(TransactionRepository transactionRepository, AccountRepository accountRepository) {
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
    }

    public Account findOrCreateAccount(String accountNumber) {
        Optional<Account> account = accountRepository.findById(accountNumber);
        if(account.isEmpty()) {
            Account newAccount = Account.createNew(accountNumber);
            accountRepository.save(newAccount);
            return newAccount;
        }
        return account.get();
    }

    public Account updateAccount(Account account) {
        accountRepository.save(account);
        return account;
    }

    public Account updateAccountBalance(Transaction transaction) {
        Account account = findOrCreateAccount(transaction.accountNumber());

        BigDecimal newBalance = calculateNewBalance(account, transaction);

        Account updatedAccount = new Account(
                account.accountNumber(),
                newBalance,
                account.createdDate()
        );

        accountRepository.save(updatedAccount);
        return updatedAccount;
    }

    public int getAccountTransactionCount(String accountNumber) {
        return transactionRepository.findByAccountNumber(accountNumber).size();
    }

    private BigDecimal calculateNewBalance(Account account, Transaction transaction) {
        return switch (transaction.type()) {
            case DEPOSIT, INTEREST ->
                    account.balance().add(transaction.amount());
            case WITHDRAWAL -> {
                BigDecimal newBalance = account.balance().subtract(transaction.amount());
                if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
                    throw new IllegalStateException("Insufficient funds");
                }
                yield newBalance;
            }
        };
    }

    public BigDecimal getAccountBalance(String accountNumber) {
        return accountRepository.findById(accountNumber)
                .map(Account::balance)
                .orElse(BigDecimal.ZERO);
    }
}
