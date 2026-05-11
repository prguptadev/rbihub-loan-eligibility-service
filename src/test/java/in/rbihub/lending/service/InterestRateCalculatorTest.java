package in.rbihub.lending.service;

import in.rbihub.lending.config.LendingPropertiesFixtures;
import in.rbihub.lending.domain.Applicant;
import in.rbihub.lending.domain.EmploymentType;
import in.rbihub.lending.domain.LoanApplication;
import in.rbihub.lending.domain.LoanPurpose;
import in.rbihub.lending.domain.LoanRequest;
import in.rbihub.lending.domain.RiskBand;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class InterestRateCalculatorTest {

    private final InterestRateCalculator calculator = new InterestRateCalculator(LendingPropertiesFixtures.standardPolicy());

    @Test
    void salariedLowRiskSmallLoanGetsBaseRateOnly() {
        BigDecimal rate = calculator.finalAnnualRate(
                application(EmploymentType.SALARIED, new BigDecimal("500000")),
                RiskBand.LOW
        );
        assertThat(rate).isEqualByComparingTo("12.00");
    }

    @Test
    void mediumRiskAddsOneAndHalfPercent() {
        BigDecimal rate = calculator.finalAnnualRate(
                application(EmploymentType.SALARIED, new BigDecimal("500000")),
                RiskBand.MEDIUM
        );
        assertThat(rate).isEqualByComparingTo("13.50");
    }

    @Test
    void highRiskAddsThreePercent() {
        BigDecimal rate = calculator.finalAnnualRate(
                application(EmploymentType.SALARIED, new BigDecimal("500000")),
                RiskBand.HIGH
        );
        assertThat(rate).isEqualByComparingTo("15.00");
    }

    @Test
    void selfEmployedAddsOnePercentOnTop() {
        BigDecimal rate = calculator.finalAnnualRate(
                application(EmploymentType.SELF_EMPLOYED, new BigDecimal("500000")),
                RiskBand.LOW
        );
        assertThat(rate).isEqualByComparingTo("13.00");
    }

    @Test
    void loanAboveTenLakhAddsHalfPercent() {
        BigDecimal rate = calculator.finalAnnualRate(
                application(EmploymentType.SALARIED, new BigDecimal("1000001")),
                RiskBand.LOW
        );
        assertThat(rate).isEqualByComparingTo("12.50");
    }

    @Test
    void exactlyTenLakhDoesNotAttractLoanSizePremium() {
        BigDecimal rate = calculator.finalAnnualRate(
                application(EmploymentType.SALARIED, new BigDecimal("1000000")),
                RiskBand.LOW
        );
        assertThat(rate).isEqualByComparingTo("12.00");
    }

    @Test
    void allPremiumsStackForHighRiskSelfEmployedLargeLoan() {
        BigDecimal rate = calculator.finalAnnualRate(
                application(EmploymentType.SELF_EMPLOYED, new BigDecimal("2500000")),
                RiskBand.HIGH
        );
        // 12 + 3 (HIGH) + 1 (self-employed) + 0.5 (large loan)
        assertThat(rate).isEqualByComparingTo("16.50");
    }

    @Test
    void resultIsScaledToTwoDecimals() {
        BigDecimal rate = calculator.finalAnnualRate(
                application(EmploymentType.SALARIED, new BigDecimal("500000")),
                RiskBand.MEDIUM
        );
        assertThat(rate.scale()).isEqualTo(2);
    }

    private LoanApplication application(EmploymentType employmentType, BigDecimal loanAmount) {
        Applicant applicant = new Applicant("Test", 30, new BigDecimal("100000"), employmentType, 720);
        LoanRequest loan = new LoanRequest(loanAmount, 36, LoanPurpose.PERSONAL);
        return new LoanApplication(applicant, loan);
    }
}
