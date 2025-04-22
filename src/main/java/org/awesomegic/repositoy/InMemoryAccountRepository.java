package org.awesomegic.repositoy;

import org.awesomegic.model.Account;
import org.awesomegic.repositoy.InMemoryRepository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryAccountRepository implements InMemoryRepository<Account,String>, AccountRepository {
    private final Map<String, Account> accountMap = new ConcurrentHashMap<>();

    @Override
    public Account save(Account account) {
        accountMap.put(account.accountNumber(), account);
        return account;
    }

    @Override
    public Optional<Account> findById(String accountNumber) {
        return Optional.ofNullable(accountMap.get(accountNumber));
    }

    @Override
    public List<Account> findAll() {
        return List.copyOf(accountMap.values());
    }

    @Override
    public boolean deleteById(String accountNumber) {
        return accountMap.remove(accountNumber) != null;
    }

}
