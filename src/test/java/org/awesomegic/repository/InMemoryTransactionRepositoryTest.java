package org.awesomegic.repository;

import org.awesomegic.model.Transaction;
import org.awesomegic.repositoy.InMemoryTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class InMemoryTransactionRepositoryTest {

    private InMemoryTransactionRepository repository;

    private Transaction transaction1;
    private Transaction transaction2;
    private Transaction transaction3;

    private final LocalDate date1 = LocalDate.of(2025, 1, 15);
    private final LocalDate date2 = LocalDate.of(2025, 2, 20);
    private final LocalDate date3 = LocalDate.of(2025, 3, 25);
    private final String accountNumber1 = "ACC-001";
    private final String accountNumber2 = "ACC-002";

    @BeforeEach
    void setUp() {
        repository = new InMemoryTransactionRepository();

        transaction1 = new Transaction("TR-001", date1, accountNumber1, Transaction.TransactionType.DEPOSIT, BigDecimal.valueOf(100.0), BigDecimal.valueOf(100.0));
        transaction2 = new Transaction("TR-002", date2, accountNumber1, Transaction.TransactionType.WITHDRAWAL, BigDecimal.valueOf(50.0), BigDecimal.valueOf(50.0));
        transaction3 = new Transaction("TR-003", date3, accountNumber2, Transaction.TransactionType.DEPOSIT, BigDecimal.valueOf(200.0), BigDecimal.valueOf(200.0));
    }



    @Nested
    @DisplayName("save method tests")
    class SaveTests {
        @Test
        @DisplayName("should throw exception when transaction has empty ID")
        void shouldThrowExceptionWhenTransactionHasNoId() {
            assertThrows(IllegalArgumentException.class, () -> {
                new Transaction("", date1, accountNumber1,
                        Transaction.TransactionType.DEPOSIT,
                        BigDecimal.valueOf(100.0),
                        BigDecimal.valueOf(100.0));
            }, "Should throw exception when transaction has empty ID");
        }

        @Test
        @DisplayName("should throw exception when transaction has null ID")
        void shouldThrowExceptionWhenTransactionHasNullId() {
            assertThrows(IllegalArgumentException.class, () -> {
                new Transaction(null, date1, accountNumber1,
                        Transaction.TransactionType.DEPOSIT,
                        BigDecimal.valueOf(100.0),
                        BigDecimal.valueOf(100.0));
            }, "Should throw exception when transaction has null ID");
        }

        @Test
        @DisplayName("should save a transaction and return a new instance")
        void shouldSaveTransactionAndReturnNewInstance() {
            Transaction savedTransaction = repository.save(transaction1);

            assertNotSame(transaction1, savedTransaction, "Should return a new instance, not the original");
            assertEquals(transaction1.id(), savedTransaction.id(), "IDs should match");
            assertEquals(transaction1.date(), savedTransaction.date(), "Dates should match");
            assertEquals(transaction1.accountNumber(), savedTransaction.accountNumber(), "Account numbers should match");
            assertEquals(transaction1.type(), savedTransaction.type(), "Types should match");
            assertEquals(transaction1.amount(), savedTransaction.amount(), "Amounts should match");
            assertEquals(transaction1.balance(), savedTransaction.balance(), "Balances should match");

            assertTrue(repository.findById("TR-001").isPresent(), "Transaction should be retrievable after saving");
        }
    }

    @Nested
    @DisplayName("findById method tests")
    class FindByIdTests {

        @Test
        @DisplayName("should return empty when transaction doesn't exist")
        void shouldReturnEmptyWhenTransactionDoesNotExist() {
            Optional<Transaction> result = repository.findById("NOT THERE");

            assertTrue(result.isEmpty(), "Should return empty Optional for non-existent transaction ID");
        }

        @Test
        @DisplayName("should find transaction by ID when it exists")
        void shouldFindTransactionByIdWhenItExists() {
            repository.save(transaction1);

            Optional<Transaction> result = repository.findById("TR-001");

            assertTrue(result.isPresent(), "Should find the transaction");
            assertEquals("TR-001", result.get().id(), "Should return transaction with correct ID");
            assertEquals(date1, result.get().date(), "Should return transaction with correct date");
            assertEquals(accountNumber1, result.get().accountNumber(), "Should return transaction with correct account number");
        }
    }

    @Nested
    @DisplayName("findAll method tests")
    class FindAllTests {

        @Test
        @DisplayName("should return empty list when no transactions exist")
        void shouldReturnEmptyListWhenNoTransactionsExist() {
            List<Transaction> transactions = repository.findAll();

            assertTrue(transactions.isEmpty(), "Should return empty list when no transactions exist");
        }

        @Test
        @DisplayName("should return all transactions")
        void shouldReturnAllTransactions() {
            repository.save(transaction1);
            repository.save(transaction2);
            repository.save(transaction3);

            List<Transaction> transactions = repository.findAll();

            assertEquals(3, transactions.size(), "Should return all transactions");
            assertTrue(transactions.stream().anyMatch(t -> t.id().equals("TR-001")), "Should contain first transaction");
            assertTrue(transactions.stream().anyMatch(t -> t.id().equals("TR-002")), "Should contain second transaction");
            assertTrue(transactions.stream().anyMatch(t -> t.id().equals("TR-003")), "Should contain third transaction");
        }
    }

    @Nested
    @DisplayName("deleteById method tests")
    class DeleteByIdTests {

        @Test
        @DisplayName("should return false when transaction doesn't exist")
        void shouldReturnFalseWhenTransactionDoesNotExist() {
            boolean result = repository.deleteById("NOT THERE");

            assertFalse(result, "Should return false when trying to delete non-existent transaction");
        }

        @Test
        @DisplayName("should delete transaction and return true when it exists")
        void shouldDeleteTransactionAndReturnTrue() {
            repository.save(transaction1);

            boolean result = repository.deleteById("TR-001");

            assertTrue(result, "Should return true when transaction is deleted");
            assertTrue(repository.findById("TR-001").isEmpty(), "Transaction should no longer exist");
        }
    }

    @Nested
    @DisplayName("findByAccountNumber method tests")
    class FindByAccountNumberTests {

        @Test
        @DisplayName("should return empty list when no transactions exist for the account")
        void shouldReturnEmptyListWhenNoTransactionsExistForAccount() {
            List<Transaction> transactions = repository.findByAccountNumber("NON-EXISTENT");

            assertTrue(transactions.isEmpty(), "Should return empty list when no transactions exist for the account");
        }

        @Test
        @DisplayName("should return all transactions for the account")
        void shouldReturnAllTransactionsForAccount() {
            repository.save(transaction1);
            repository.save(transaction2);
            repository.save(transaction3);

            List<Transaction> transactions = repository.findByAccountNumber(accountNumber1);

            assertEquals(2, transactions.size(), "Should return only transactions for the specified account");
            assertTrue(transactions.stream().allMatch(t -> t.accountNumber().equals(accountNumber1)),
                    "All transactions should be for the specified account");
            assertTrue(transactions.stream().anyMatch(t -> t.id().equals("TR-001")), "Should contain first transaction");
            assertTrue(transactions.stream().anyMatch(t -> t.id().equals("TR-002")), "Should contain second transaction");
        }
    }

    @Nested
    @DisplayName("findByAccountNumberAndDateRange method tests")
    class FindByAccountNumberAndDateRangeTests {

        @Test
        @DisplayName("should return empty list when no transactions match criteria")
        void shouldReturnEmptyListWhenNoTransactionsMatchCriteria() {
            repository.save(transaction1);

            List<Transaction> result1 = repository.findByAccountNumberAndDateRange("NON-EXISTENT",
                    date1.minusDays(10), date1.plusDays(10));

            assertTrue(result1.isEmpty(), "Should return empty list for non-existent account");

            List<Transaction> result2 = repository.findByAccountNumberAndDateRange(accountNumber1,
                    date1.minusDays(30), date1.minusDays(20));

            assertTrue(result2.isEmpty(), "Should return empty list for date range before transaction");

            List<Transaction> result3 = repository.findByAccountNumberAndDateRange(accountNumber1,
                    date1.plusDays(20), date1.plusDays(30));

            assertTrue(result3.isEmpty(), "Should return empty list for date range after transaction");
        }

        @Test
        @DisplayName("should return transactions that match account and date range")
        void shouldReturnTransactionsMatchingAccountAndDateRange() {
            repository.save(transaction1);
            repository.save(transaction2);
            repository.save(transaction3);

            List<Transaction> result = repository.findByAccountNumberAndDateRange(accountNumber1,
                    date1.minusDays(5), date2.plusDays(5));

            assertEquals(2, result.size(), "Should return two transactions");
            assertEquals("TR-001", result.get(0).id(), "First transaction should be the earliest one");
            assertEquals("TR-002", result.get(1).id(), "Second transaction should be the later one");
        }

        @Test
        @DisplayName("should include transactions on boundary dates")
        void shouldIncludeTransactionsOnBoundaryDates() {
            repository.save(transaction1);

            List<Transaction> result = repository.findByAccountNumberAndDateRange(accountNumber1,
                    date1, date1);

            assertEquals(1, result.size(), "Should include the transaction with exactly matching date");
            assertEquals("TR-001", result.get(0).id(), "Should return the correct transaction");
        }
    }

    @Nested
    @DisplayName("findTransactionsByDate method tests")
    class FindTransactionsByDateTests {

        @Test
        @DisplayName("should return empty list when no transactions on the date")
        void shouldReturnEmptyListWhenNoTransactionsOnDate() {
            repository.save(transaction1);

            List<Transaction> result = repository.findTransactionsByDate(date1.plusDays(1));

            assertTrue(result.isEmpty(), "Should return empty list when no transactions on the date");
        }

        @Test
        @DisplayName("should return transactions on the specified date")
        void shouldReturnTransactionsOnSpecifiedDate() {
            repository.save(transaction1);
            repository.save(transaction2);

            Transaction transaction4 = new Transaction("TR-004", date1, accountNumber1, Transaction.TransactionType.DEPOSIT, BigDecimal.valueOf(100.0), BigDecimal.valueOf(100.0));
            repository.save(transaction4);

            List<Transaction> result = repository.findTransactionsByDate(date1);

            assertEquals(2, result.size(), "Should return two transactions");
            assertTrue(result.stream().allMatch(t -> t.date().equals(date1)),
                    "All transactions should have the specified date");
            assertTrue(result.stream().anyMatch(t -> t.id().equals("TR-001")),
                    "Should include first transaction");
            assertTrue(result.stream().anyMatch(t -> t.id().equals("TR-004")),
                    "Should include other transaction with same date");
        }
    }
}
