package org.awesomegic.service;

import org.awesomegic.model.InterestRule;
import org.awesomegic.repositoy.InterestRuleRepository;
import org.awesomegic.util.InputValidator;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class InterestRuleService {
    public record InterestRuleRequest(
            LocalDate effectiveDate,
            String ruleId,
            BigDecimal interestRate
    ) {}

    private final InterestRuleRepository interestRuleRepository;

    public InterestRuleService(InterestRuleRepository interestRuleRepository) {
        this.interestRuleRepository = interestRuleRepository;
    }

    public void processInterestRule(String input) {
        InterestRuleRequest request = parseInterestRuleInput(input);
        validateInterestRuleRequest(request);

        handleExistingRulesOnSameDate(request.effectiveDate());

        InterestRule interestRule = new InterestRule(
                request.effectiveDate(),
                request.ruleId(),
                request.interestRate().setScale(2, RoundingMode.HALF_UP)
        );

        interestRuleRepository.save(interestRule);
    }

    protected InterestRuleRequest parseInterestRuleInput(String input) {
        String[] parts = input.split("\\s+");
        if(parts.length != 3) {
            throw new IllegalArgumentException("Invalid input format");
        }

        return new InterestRuleRequest(
                InputValidator.parseAndValidateDate(parts[0]),
                parts[1],
                new BigDecimal(parts[2])
        );
    }

    protected void validateInterestRuleRequest(InterestRuleRequest request) {
        BigDecimal rate = request.interestRate();
        if (rate.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Interest rate must be positive");
        }
        if (rate.compareTo(BigDecimal.valueOf(100)) >= 0) {
            throw new IllegalArgumentException("Interest rate must be less than 100%");
        }
        if(rate.scale() > 2) {
            throw new IllegalArgumentException("Interest rate cannot have more than 2 decimal places");
        }
    }

    protected void handleExistingRulesOnSameDate(LocalDate effectiveDate) {
        interestRuleRepository.findAll().stream()
                .filter(rule -> rule.effectiveDate().equals(effectiveDate))
                .forEach(rule -> interestRuleRepository.deleteById(rule.ruleId()));
    }

    public List<InterestRule> getAllInterestRules() {
        return interestRuleRepository.findAll();
    }

    public Optional<InterestRule> findApplicableInterestRule(LocalDate date) {
        return interestRuleRepository.findAll().stream()
                .filter(rule -> !rule.effectiveDate().isAfter(date))
                .max((r1, r2) -> r1.effectiveDate().compareTo(r2.effectiveDate()));
    }

    public BigDecimal calculateProratedInterest(
            BigDecimal balance,
            InterestRule interestRule,
            long periodDays) {

        return balance
                .multiply(interestRule.interestRate().divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP))
                .multiply(BigDecimal.valueOf(periodDays))
                .divide(BigDecimal.valueOf(365), 10, RoundingMode.HALF_UP)
                .setScale(2, RoundingMode.HALF_UP);
    }

}
