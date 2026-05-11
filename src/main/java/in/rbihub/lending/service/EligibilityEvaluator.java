package in.rbihub.lending.service;

import in.rbihub.lending.config.LendingProperties;
import in.rbihub.lending.domain.LoanApplication;
import in.rbihub.lending.domain.RejectionReason;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;


@Component
public class EligibilityEvaluator {

    private static final Logger log = LoggerFactory.getLogger(EligibilityEvaluator.class);

    private final LendingProperties policy;

    public EligibilityEvaluator(LendingProperties policy) {
        this.policy = policy;
    }

    public List<RejectionReason> evaluate(LoanApplication application,
                                          Optional<BigDecimal> projectedEmi) {
        List<RejectionReason> reasons = new ArrayList<>();

        int score = application.applicant().creditScore();
        if (score < policy.minCreditScore()) {
            log.debug("Eligibility: credit score {} < {} -> CREDIT_SCORE_BELOW_MINIMUM",
                    score, policy.minCreditScore());
            reasons.add(RejectionReason.CREDIT_SCORE_BELOW_MINIMUM);
        } else {
            log.debug("Eligibility: credit score {} >= {} (pass)", score, policy.minCreditScore());
        }

        // Stay in integer-month arithmetic so the boundary is unambiguous - no
        // fractional-year rounding surprises around the 65-year cut-off.
        int monthsAtMaturity = application.applicant().age() * 12 + application.loan().tenureMonths();
        int maxAtMaturityMonths = policy.maxAgeAtMaturityYears() * 12;
        if (monthsAtMaturity > maxAtMaturityMonths) {
            log.debug("Eligibility: maturity months {} > {} -> AGE_TENURE_LIMIT_EXCEEDED",
                    monthsAtMaturity, maxAtMaturityMonths);
            reasons.add(RejectionReason.AGE_TENURE_LIMIT_EXCEEDED);
        } else {
            log.debug("Eligibility: maturity months {} <= {} (pass)", monthsAtMaturity, maxAtMaturityMonths);
        }

        // EMI check is conditional - sub-floor scores skip rate/EMI computation entirely.
        projectedEmi.ifPresent(emi -> {
            BigDecimal cap = application.applicant().monthlyIncome().multiply(policy.emiToIncomeHardCap());
            if (emi.compareTo(cap) > 0) {
                log.debug("Eligibility: EMI {} > hard-cap {} -> EMI_EXCEEDS_60_PERCENT", emi, cap);
                reasons.add(RejectionReason.EMI_EXCEEDS_60_PERCENT);
            } else {
                log.debug("Eligibility: EMI {} <= hard-cap {} (pass)", emi, cap);
            }
        });

        return Collections.unmodifiableList(reasons);
    }
}
