package org.awesomegic.repositoy;

import org.awesomegic.model.Account;

import java.util.List;
import java.util.Optional;

public interface AccountRepository {

    Account save(Account account);

    Optional<Account> findById(String accountNumber);

    List<Account> findAll();

    boolean deleteById(String accountNumber);
}
