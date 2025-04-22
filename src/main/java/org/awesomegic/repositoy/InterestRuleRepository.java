package org.awesomegic.repositoy;

import org.awesomegic.model.InterestRule;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface InterestRuleRepository {
    InterestRule save(InterestRule interestRule);
    Optional<InterestRule> findById(String ruleId);
    List<InterestRule> findAll();
    boolean deleteById(String ruleId);
    Optional<InterestRule> findMostRecentRuleBeforeDate(LocalDate date);
}
