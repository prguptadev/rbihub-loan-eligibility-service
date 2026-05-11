package in.rbihub.lending.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;


@ConfigurationProperties(prefix = "lending")
public record LendingProperties(

        // ---- Pricing ----
        BigDecimal baseAnnualRatePercent,

        // Risk-band thresholds (inclusive lower bound for each band).
        int lowRiskScoreFloor,
        int mediumRiskScoreFloor,
        int highRiskScoreFloor,

        // Premiums on top of the base rate.
        BigDecimal mediumRiskPremium,
        BigDecimal highRiskPremium,
        BigDecimal selfEmployedPremium,
        BigDecimal largeLoanThreshold,
        BigDecimal largeLoanPremium,

        // ---- Eligibility gates ----
        int minCreditScore,
        int maxAgeAtMaturityYears,
        BigDecimal emiToIncomeHardCap,

        // ---- Offer viability ----
        BigDecimal emiToIncomeOfferCap
) {
}
