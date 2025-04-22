package org.awesomegic.service;

import org.awesomegic.model.InterestRule;
import org.awesomegic.repositoy.InterestRuleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class InterestRuleServiceTest {
    @Mock
    private InterestRuleRepository interestRuleRepository;

    @InjectMocks
    private InterestRuleService interestRuleService;

    @Captor
    private ArgumentCaptor<InterestRule> interestRuleCaptor;

    private final LocalDate EFFECTIVE_DATE = LocalDate.of(2025, 1, 1);
    private final String RULE_ID = "RULE-001";
    private final BigDecimal INTEREST_RATE = BigDecimal.valueOf(2.5);
    private final String VALID_INPUT = "20250101 RULE-001 2.5";

    private InterestRule testRule;

    @BeforeEach
    void setUp() {
        testRule = new InterestRule(EFFECTIVE_DATE, RULE_ID, INTEREST_RATE);
    }

    @Test
    @DisplayName("Should process valid interest rule input")
    void shouldProcessValidInterestRuleInput() {
        when(interestRuleRepository.findAll()).thenReturn(new ArrayList<>());

        interestRuleService.processInterestRule(VALID_INPUT);

        verify(interestRuleRepository).save(interestRuleCaptor.capture());
        InterestRule savedRule = interestRuleCaptor.getValue();
        assertEquals(EFFECTIVE_DATE, savedRule.effectiveDate());
        assertEquals(RULE_ID, savedRule.ruleId());
        assertEquals(0, INTEREST_RATE.compareTo(savedRule.interestRate()));
    }

    @Test
    @DisplayName("Should throw exception when input format is invalid")
    void shouldThrowExceptionWhenInputFormatIsInvalid() {
        String invalidInput = "20250101 RULE-001";

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                interestRuleService.processInterestRule(invalidInput));
        assertEquals("Invalid input format", exception.getMessage());
        verify(interestRuleRepository, never()).save(any(InterestRule.class));
    }

    @Test
    @DisplayName("Should throw exception when interest rate is not positive")
    void shouldThrowExceptionWhenInterestRateIsNotPositive() {
        String zeroRateInput = "20250101 RULE-001 0";
        String negativeRateInput = "20250101 RULE-001 -1.5";

        IllegalArgumentException exception1 = assertThrows(IllegalArgumentException.class, () ->
                interestRuleService.processInterestRule(zeroRateInput));
        assertEquals("Interest rate must be positive", exception1.getMessage());

        IllegalArgumentException exception2 = assertThrows(IllegalArgumentException.class, () ->
                interestRuleService.processInterestRule(negativeRateInput));
        assertEquals("Interest rate must be positive", exception2.getMessage());

        verify(interestRuleRepository, never()).save(any(InterestRule.class));
    }

    @Test
    @DisplayName("Should throw exception when interest rate is too high")
    void shouldThrowExceptionWhenInterestRateIsTooHigh() {
        String highRateInput = "20250101 RULE-001 100";

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                interestRuleService.processInterestRule(highRateInput));
        assertEquals("Interest rate must be less than 100%", exception.getMessage());
        verify(interestRuleRepository, never()).save(any(InterestRule.class));
    }

    @Test
    @DisplayName("Should throw exception when interest rate has too many decimal places")
    void shouldThrowExceptionWhenInterestRateHasTooManyDecimalPlaces() {
        String tooManyDecimalsInput = "20250101 RULE-001 2.555";

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                interestRuleService.processInterestRule(tooManyDecimalsInput));
        assertEquals("Interest rate cannot have more than 2 decimal places", exception.getMessage());
        verify(interestRuleRepository, never()).save(any(InterestRule.class));
    }

    @Test
    @DisplayName("Should replace existing rules with same effective date")
    void shouldReplaceExistingRulesWithSameEffectiveDate() {
        InterestRule existingRule = new InterestRule(EFFECTIVE_DATE, "OLD-RULE", BigDecimal.valueOf(1.5));
        List<InterestRule> existingRules = List.of(existingRule);

        when(interestRuleRepository.findAll()).thenReturn(existingRules);

        interestRuleService.processInterestRule(VALID_INPUT);

        verify(interestRuleRepository).deleteById("OLD-RULE");
        verify(interestRuleRepository).save(interestRuleCaptor.capture());
        InterestRule savedRule = interestRuleCaptor.getValue();
        assertEquals(EFFECTIVE_DATE, savedRule.effectiveDate());
        assertEquals(RULE_ID, savedRule.ruleId());
        assertEquals(0, INTEREST_RATE.compareTo(savedRule.interestRate()));
    }

    @Test
    @DisplayName("Should return all interest rules")
    void shouldReturnAllInterestRules() {
        List<InterestRule> rules = Arrays.asList(
                new InterestRule(LocalDate.of(2023, 1, 1), "RULE-001", BigDecimal.valueOf(2.5)),
                new InterestRule(LocalDate.of(2023, 2, 1), "RULE-002", BigDecimal.valueOf(3.0))
        );

        when(interestRuleRepository.findAll()).thenReturn(rules);

        List<InterestRule> result = interestRuleService.getAllInterestRules();

        assertEquals(2, result.size());
        assertEquals(rules, result);
        verify(interestRuleRepository).findAll();
    }

    @Test
    @DisplayName("Should find applicable interest rule for given date")
    void shouldFindApplicableInterestRuleForGivenDate() {
        List<InterestRule> rules = Arrays.asList(
                new InterestRule(LocalDate.of(2023, 1, 1), "RULE-001", BigDecimal.valueOf(2.5)),
                new InterestRule(LocalDate.of(2023, 2, 1), "RULE-002", BigDecimal.valueOf(3.0)),
                new InterestRule(LocalDate.of(2023, 3, 1), "RULE-003", BigDecimal.valueOf(3.5))
        );

        when(interestRuleRepository.findAll()).thenReturn(rules);

        Optional<InterestRule> result = interestRuleService.findApplicableInterestRule(LocalDate.of(2023, 2, 15));

        assertTrue(result.isPresent());
        assertEquals("RULE-002", result.get().ruleId());
        assertEquals(BigDecimal.valueOf(3.0), result.get().interestRate());
    }

    @Test
    @DisplayName("Should return empty when no applicable interest rule is found for date before any rule")
    void shouldReturnEmptyWhenNoApplicableInterestRuleIsFound() {
        List<InterestRule> rules = Arrays.asList(
                new InterestRule(LocalDate.of(2023, 2, 1), "RULE-001", BigDecimal.valueOf(2.5)),
                new InterestRule(LocalDate.of(2023, 3, 1), "RULE-002", BigDecimal.valueOf(3.0))
        );

        when(interestRuleRepository.findAll()).thenReturn(rules);

        Optional<InterestRule> result = interestRuleService.findApplicableInterestRule(LocalDate.of(2023, 1, 15));

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Should find the most recent applicable interest rule when multiple exist")
    void shouldFindMostRecentApplicableInterestRuleWhenMultipleExist() {
        List<InterestRule> rules = Arrays.asList(
                new InterestRule(LocalDate.of(2023, 1, 1), "RULE-001", BigDecimal.valueOf(2.5)),
                new InterestRule(LocalDate.of(2023, 2, 1), "RULE-002", BigDecimal.valueOf(3.0)),
                new InterestRule(LocalDate.of(2023, 3, 1), "RULE-003", BigDecimal.valueOf(3.5))
        );

        when(interestRuleRepository.findAll()).thenReturn(rules);

        Optional<InterestRule> result = interestRuleService.findApplicableInterestRule(LocalDate.of(2023, 3, 15));

        assertTrue(result.isPresent());
        assertEquals("RULE-003", result.get().ruleId());
    }

    @Test
    @DisplayName("Should calculate prorated interest correctly")
    void shouldCalculateProratedInterestCorrectly() {
        BigDecimal balance = BigDecimal.valueOf(10000);
        InterestRule rule = new InterestRule(
                LocalDate.of(2025, 1, 1),
                "RULE-001",
                BigDecimal.valueOf(3.65)
        );
        long periodDays = 30;

        // Expected calculation: 10000 * (3.65/100) * (30/365) = 30.00
        BigDecimal expected = BigDecimal.valueOf(30.00);

        BigDecimal result = interestRuleService.calculateProratedInterest(balance, rule, periodDays);

        assertEquals(0, expected.compareTo(result));
        assertEquals(2, result.scale());
    }

    @Test
    @DisplayName("Should calculate prorated interest as zero when balance is zero")
    void shouldCalculateProratedInterestAsZeroWhenBalanceIsZero() {
        BigDecimal balance = BigDecimal.ZERO;
        InterestRule rule = new InterestRule(
                LocalDate.of(2023, 1, 1),
                "RULE-001",
                BigDecimal.valueOf(3.65)
        );
        long periodDays = 30;

        BigDecimal result = interestRuleService.calculateProratedInterest(balance, rule, periodDays);

        assertEquals(BigDecimal.valueOf(0).setScale(2, RoundingMode.HALF_UP), result);
    }

    @Test
    @DisplayName("Should calculate prorated interest as zero when period days is zero")
    void shouldCalculateProratedInterestAsZeroWhenPeriodDaysIsZero() {
        BigDecimal balance = BigDecimal.valueOf(10000);
        InterestRule rule = new InterestRule(
                LocalDate.of(2023, 1, 1),
                "RULE-001",
                BigDecimal.valueOf(3.65)
        );
        long periodDays = 0;

        BigDecimal result = interestRuleService.calculateProratedInterest(balance, rule, periodDays);

        assertEquals(BigDecimal.valueOf(0).setScale(2, RoundingMode.HALF_UP), result);
    }

    @Test
    @DisplayName("Should correctly parse date from input string")
    void shouldCorrectlyParseDateFromInputString() {
        String input = "20250115 RULE-001 2.5";
        LocalDate expectedDate = LocalDate.of(2025, 1, 15);

        InterestRuleService.InterestRuleRequest request = interestRuleService.parseInterestRuleInput(input);

        assertEquals(expectedDate, request.effectiveDate());
    }

    @Test
    @DisplayName("Should correctly parse rule ID from input string")
    void shouldCorrectlyParseRuleIdFromInputString() {
        String input = "20250115 CUSTOM-RULE-ID 2.5";

        InterestRuleService.InterestRuleRequest request = interestRuleService.parseInterestRuleInput(input);

        assertEquals("CUSTOM-RULE-ID", request.ruleId());
    }

    @Test
    @DisplayName("Should correctly parse interest rate from input string")
    void shouldCorrectlyParseInterestRateFromInputString() {
        String input = "20250115 RULE-001 3.75";
        BigDecimal expectedRate = BigDecimal.valueOf(3.75);

        InterestRuleService.InterestRuleRequest request = interestRuleService.parseInterestRuleInput(input);

        assertEquals(expectedRate, request.interestRate());
    }
}
