package org.awesomegic.repositoy;

import org.awesomegic.model.InterestRule;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public final class InMemoryInterestRuleRepository implements InMemoryRepository<InterestRule,String>, InterestRuleRepository {

    private final Map<String,InterestRule> interestRulesMap = new ConcurrentHashMap<>();
    @Override
    public InterestRule save(InterestRule interestRule) {
        interestRulesMap.put(interestRule.ruleId(), interestRule);
        return interestRule;
    }

    @Override
    public Optional<InterestRule> findById(String ruleId) {
        return Optional.ofNullable(interestRulesMap.get(ruleId));

    }

    @Override
    public List<InterestRule> findAll() {
        return interestRulesMap.values().stream()
                .sorted(Comparator.comparing(InterestRule::effectiveDate))
                .collect(Collectors.toList());
    }

    @Override
    public boolean deleteById(String ruleId) {
        return interestRulesMap.remove(ruleId) != null;
    }

    @Override
    public Optional<InterestRule> findMostRecentRuleBeforeDate(LocalDate date) {
        return interestRulesMap.values().stream()
                .filter(rule -> !rule.effectiveDate().isAfter(date))
                .max(java.util.Comparator.comparing(InterestRule::effectiveDate));
    }
}
