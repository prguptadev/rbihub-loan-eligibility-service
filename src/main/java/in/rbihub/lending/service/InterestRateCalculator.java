package in.rbihub.lending.service;

import in.rbihub.lending.config.LendingProperties;
import in.rbihub.lending.domain.EmploymentType;
import in.rbihub.lending.domain.LoanApplication;
import in.rbihub.lending.domain.RiskBand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class InterestRateCalculator {

    private static final Logger log = LoggerFactory.getLogger(InterestRateCalculator.class);

    private final LendingProperties policy;

    public InterestRateCalculator(LendingProperties policy) {
        this.policy = policy;
    }

    public BigDecimal finalAnnualRate(LoanApplication application, RiskBand riskBand) {
        BigDecimal risk = riskPremium(riskBand);
        BigDecimal employment = employmentPremium(application.applicant().employmentType());
        BigDecimal size = loanSizePremium(application.loan().amount());
        BigDecimal rate = policy.baseAnnualRatePercent()
                .add(risk).add(employment).add(size)
                .setScale(2, RoundingMode.HALF_UP);

        if (log.isDebugEnabled()) {
            log.debug("Rate composition: base={} + risk={}({}) + employment={}({}) + loanSize={}(amount={}) = {}",
                    policy.baseAnnualRatePercent(),
                    risk, riskBand,
                    employment, application.applicant().employmentType(),
                    size, application.loan().amount(),
                    rate);
        }
        return rate;
    }

    private BigDecimal riskPremium(RiskBand riskBand) {
        return switch (riskBand) {
            case LOW -> BigDecimal.ZERO;
            case MEDIUM -> policy.mediumRiskPremium();
            case HIGH -> policy.highRiskPremium();
        };
    }

    private BigDecimal employmentPremium(EmploymentType employmentType) {
        return switch (employmentType) {
            case SALARIED -> BigDecimal.ZERO;
            case SELF_EMPLOYED -> policy.selfEmployedPremium();
        };
    }

    private BigDecimal loanSizePremium(BigDecimal amount) {
        // Strict greater-than per spec - exactly the threshold attracts no premium.
        return amount.compareTo(policy.largeLoanThreshold()) > 0
                ? policy.largeLoanPremium()
                : BigDecimal.ZERO;
    }
}
