package org.awesomegic.repository;

import org.awesomegic.model.InterestRule;
import org.awesomegic.repositoy.InMemoryInterestRuleRepository;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

@ExtendWith(MockitoExtension.class)
class InMemoryInterestRuleRepositoryTest {

    private InMemoryInterestRuleRepository repository;

    private InterestRule rule1;
    private InterestRule rule2;
    private InterestRule rule3;

    private final LocalDate date1 = LocalDate.of(2025, 1, 1);
    private final LocalDate date2 = LocalDate.of(2025, 6, 1);
    private final LocalDate date3 = LocalDate.of(2025, 12, 1);

    @BeforeEach
    void setUp() {
        repository = new InMemoryInterestRuleRepository();

        rule1 = new InterestRule(date1,"RULE-001", BigDecimal.ONE);
        rule2 = new InterestRule(date2,"RULE-002", BigDecimal.ONE);
        rule3 = new InterestRule(date3,"RULE-003", BigDecimal.ONE);
    }


    @Nested
    @DisplayName("save method tests")
    class SaveTests {
        @Test
        @DisplayName("should save a rule and return it")
        void shouldSaveRuleAndReturnIt() {
            InterestRule savedRule = repository.save(rule1);

            assertEquals(rule1, savedRule, "The returned rule should be the same as the one saved");
            assertTrue(repository.findById("RULE-001").isPresent(), "The rule should be retrievable after saving");
        }

        @Test
        @DisplayName("should update an existing rule")
        void shouldUpdateExistingRule() {
            repository.save(rule1);

            InterestRule updatedRule = repository.save(rule1);

            assertEquals(rule1, updatedRule, "The returned rule should be the updated one");
            assertEquals(1, repository.findAll().size(), "There should still be only one rule");
        }
    }

    @Nested
    @DisplayName("findById method tests")
    class FindByIdTests {

        @Test
        @DisplayName("should return empty when rule doesn't exist")
        void shouldReturnEmptyWhenRuleDoesNotExist() {
            Optional<InterestRule> result = repository.findById("NONE");

            assertTrue(result.isEmpty(), "Should return empty Optional for non-existent rule ID");
        }

        @Test
        @DisplayName("should find rule by ID when it exists")
        void shouldFindRuleByIdWhenItExists() {
            repository.save(rule1);

            Optional<InterestRule> result = repository.findById("RULE-001");

            assertTrue(result.isPresent(), "Should find the rule");
            assertEquals(rule1, result.get(), "Should return the correct rule");
        }
    }

    @Nested
    @DisplayName("findAll method tests")
    class FindAllTests {
        @Test
        @DisplayName("should return empty list when no rules exist")
        void shouldReturnEmptyListWhenNoRulesExist() {
            List<InterestRule> rules = repository.findAll();

            assertTrue(rules.isEmpty(), "Should return empty list when no rules exist");
        }

        @Test
        @DisplayName("should return all rules sorted by effective date")
        void shouldReturnAllRulesSortedByEffectiveDate() {
            repository.save(rule3);
            repository.save(rule1);
            repository.save(rule2);

            List<InterestRule> rules = repository.findAll();

            assertEquals(3, rules.size(), "Should return all rules");
            assertEquals(rule1, rules.get(0), "First rule should be the earliest one (January)");
            assertEquals(rule2, rules.get(1), "Second rule should be the middle one (June)");
            assertEquals(rule3, rules.get(2), "Third rule should be the latest one (December)");
        }
    }

    @Nested
    @DisplayName("deleteById method tests")
    class DeleteByIdTests {

        @Test
        @DisplayName("should return false when rule doesn't exist")
        void shouldReturnFalseWhenRuleDoesNotExist() {
            boolean result = repository.deleteById("NONE");

            assertFalse(result, "Should return false when trying to delete non-existent rule");
        }

        @Test
        @DisplayName("should delete rule and return true when it exists")
        void shouldDeleteRuleAndReturnTrue() {
            repository.save(rule1);

            boolean result = repository.deleteById("RULE-001");

            assertTrue(result, "Should return true when rule is deleted");
            assertTrue(repository.findById("RULE-001").isEmpty(), "Rule should no longer exist");
        }
    }

    @Nested
    @DisplayName("findMostRecentRuleBeforeDate method tests")
    class FindMostRecentRuleBeforeDateTests {

        @Test
        @DisplayName("should return empty when no rules exist")
        void shouldReturnEmptyWhenNoRulesExist() {
            Optional<InterestRule> result = repository.findMostRecentRuleBeforeDate(LocalDate.now());

            assertTrue(result.isEmpty(), "Should return empty when no rules exist");
        }

        @Test
        @DisplayName("should return empty when no rules are before the given date")
        void shouldReturnEmptyWhenNoRulesBeforeDate() {
            repository.save(rule1);

            Optional<InterestRule> result = repository.findMostRecentRuleBeforeDate(LocalDate.of(2022, 12, 31));

            assertTrue(result.isEmpty(), "Should return empty when no rules are before the given date");
        }

        @Test
        @DisplayName("should return the most recent rule before the given date")
        void shouldReturnMostRecentRuleBeforeDate() {
            repository.save(rule1);
            repository.save(rule2);
            repository.save(rule3);
            
            Optional<InterestRule> result = repository.findMostRecentRuleBeforeDate(LocalDate.of(2025, 10, 1));

            assertTrue(result.isPresent(), "Should find a rule");
            assertEquals(rule2, result.get(), "Should return the most recent rule before the given date (rule2)");
        }

        @Test
        @DisplayName("should return the rule when the date matches exactly")
        void shouldReturnRuleWhenDateMatchesExactly() {
            repository.save(rule1);
            repository.save(rule2);

            Optional<InterestRule> result = repository.findMostRecentRuleBeforeDate(date2);

            assertTrue(result.isPresent(), "Should find a rule");
            assertEquals(rule2, result.get(), "Should return the rule with the matching date");
        }
    }
}
