package org.awesomegic.repository;

import org.awesomegic.model.Account;
import org.awesomegic.repositoy.InMemoryAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InMemoryAccountRepositoryTest {

    private InMemoryAccountRepository repository;
    private Account testAccount1;
    private Account testAccount2;

    @BeforeEach
    void setUp() {
        repository = new InMemoryAccountRepository();

        testAccount1 = new Account("ACC-001", BigDecimal.ZERO,LocalDate.now());
        testAccount2 = new Account("ACC-002", BigDecimal.ZERO,LocalDate.now());
    }


    @Nested
    @DisplayName("Save method tests")
    class SaveTests {
        @Test
        @DisplayName("Should save an account and return it")
        void shouldSaveAccountAndReturnIt() {
            Account savedAccount = repository.save(testAccount1);

            assertEquals(testAccount1, savedAccount, "The returned account should be the same as the one saved");
            assertTrue(repository.findById("ACC-001").isPresent(), "The account should be retrievable after saving");
        }

        @Test
        @DisplayName("Should update an existing account")
        void shouldUpdateExistingAccount() {
            repository.save(testAccount1);
            Account updatedAccount = repository.save(testAccount1);

            assertEquals(testAccount1, updatedAccount, "The returned account should be the updated one");
            assertEquals(1, repository.findAll().size(), "There should still be only one account");
        }
    }

    @Nested
    @DisplayName("findById method tests")
    class FindByIdTests {
        @Test
        @DisplayName("Should return empty when account doesn't exist")
        void shouldReturnEmptyWhenAccountDoesNotExist() {
            Optional<Account> result = repository.findById("NOT THERE");

            assertTrue(result.isEmpty(), "Should return empty Optional for a non-existent account ID");
        }

        @Test
        @DisplayName("Should find account by ID when it exists")
        void shouldFindAccountByIdWhenItExists() {
            repository.save(testAccount1);

            Optional<Account> result = repository.findById("ACC-001");

            assertTrue(result.isPresent(), "Should find the account");
            assertEquals(testAccount1, result.get(), "Should return the correct account");
        }
    }

    @Nested
    @DisplayName("findAll method tests")
    class FindAllTests {
        @Test
        @DisplayName("Should return empty list when no accounts exist")
        void shouldReturnEmptyListWhenNoAccountsExist() {
            List<Account> accounts = repository.findAll();

            assertTrue(accounts.isEmpty(), "Should return empty list when no accounts exist");
        }

        @Test
        @DisplayName("Should return all accounts")
        void shouldReturnAllAccounts() {
            repository.save(testAccount1);
            repository.save(testAccount2);

            List<Account> accounts = repository.findAll();

            assertEquals(2, accounts.size(), "Should return all accounts");
            assertTrue(accounts.contains(testAccount1), "Should contain first test account");
            assertTrue(accounts.contains(testAccount2), "Should contain second test account");
        }

        @Test
        @DisplayName("Should return immutable list")
        void shouldReturnImmutableList() {
            repository.save(testAccount1);
            repository.save(testAccount2);

            List<Account> accounts = repository.findAll();

            assertThrows(UnsupportedOperationException.class, () -> accounts.add(testAccount1),
                    "Should return an immutable list");
        }
    }

    @Nested
    @DisplayName("deleteById method tests")
    class DeleteByIdTests {

        @Test
        @DisplayName("Should return false when account doesn't exist")
        void shouldReturnFalseWhenAccountDoesNotExist() {
            boolean result = repository.deleteById("NON-EXISTENT");

            assertFalse(result, "Should return false when trying to delete non-existent account");
        }

        @Test
        @DisplayName("should delete account and return true when it exists")
        void shouldDeleteAccountAndReturnTrue() {
            repository.save(testAccount1);

            boolean result = repository.deleteById("ACC-001");

            assertTrue(result, "Should return true when account is deleted");
            assertTrue(repository.findById("ACC-001").isEmpty(), "Account should no longer exist");
        }

        @Test
        @DisplayName("Should only delete specified account")
        void shouldOnlyDeleteSpecifiedAccount() {
            repository.save(testAccount1);
            repository.save(testAccount2);

            repository.deleteById("ACC-001");

            assertEquals(1, repository.findAll().size(), "Should only have one account left");
            assertTrue(repository.findById("ACC-002").isPresent(), "Other account should still exist");
        }
    }
}
