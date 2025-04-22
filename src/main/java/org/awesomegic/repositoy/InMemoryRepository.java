package org.awesomegic.repositoy;

public sealed interface InMemoryRepository<T,ID> extends Repository<T,ID> permits
        InMemoryTransactionRepository, InMemoryInterestRuleRepository, InMemoryAccountRepository {
}
