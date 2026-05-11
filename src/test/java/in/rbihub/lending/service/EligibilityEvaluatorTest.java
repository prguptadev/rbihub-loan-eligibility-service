package in.rbihub.lending.service;

import in.rbihub.lending.config.LendingPropertiesFixtures;
import in.rbihub.lending.domain.Applicant;
import in.rbihub.lending.domain.EmploymentType;
import in.rbihub.lending.domain.LoanApplication;
import in.rbihub.lending.domain.LoanPurpose;
import in.rbihub.lending.domain.LoanRequest;
import in.rbihub.lending.domain.RejectionReason;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class EligibilityEvaluatorTest {

    private final EligibilityEvaluator evaluator = new EligibilityEvaluator(LendingPropertiesFixtures.standardPolicy());

    @Test
    void cleanProfileWithSafeEmiHasNoReasons() {
        LoanApplication application = application(35, new BigDecimal("100000"), 720, 36);

        var reasons = evaluator.evaluate(application, Optional.of(new BigDecimal("20000")));

        assertThat(reasons).isEmpty();
    }

    @Test
    void creditScoreUnderSixHundredIsRejected() {
        LoanApplication application = application(35, new BigDecimal("100000"), 599, 36);

        var reasons = evaluator.evaluate(application, Optional.empty());

        assertThat(reasons).containsExactly(RejectionReason.CREDIT_SCORE_BELOW_MINIMUM);
    }

    @Test
    void scoreOfExactlySixHundredIsAccepted() {
        LoanApplication application = application(35, new BigDecimal("100000"), 600, 36);

        var reasons = evaluator.evaluate(application, Optional.of(new BigDecimal("20000")));

        assertThat(reasons).isEmpty();
    }

    @Test
    void agePlusTenureExceedingSixtyFiveYearsIsRejected() {
        // 60-year-old asking for 72 months -> matures at 66, should fail.
        LoanApplication application = application(60, new BigDecimal("100000"), 720, 72);

        var reasons = evaluator.evaluate(application, Optional.of(new BigDecimal("20000")));

        assertThat(reasons).contains(RejectionReason.AGE_TENURE_LIMIT_EXCEEDED);
    }

    @Test
    void maturityExactlyAtSixtyFiveIsStillAcceptable() {
        // 60-year-old, 60-month tenure -> matures at exactly 65 (rule is strict >).
        LoanApplication application = application(60, new BigDecimal("100000"), 720, 60);

        var reasons = evaluator.evaluate(application, Optional.of(new BigDecimal("20000")));

        assertThat(reasons).doesNotContain(RejectionReason.AGE_TENURE_LIMIT_EXCEEDED);
    }

    @Test
    void emiAboveSixtyPercentOfIncomeIsRejected() {
        LoanApplication application = application(35, new BigDecimal("50000"), 720, 36);
        // 60% of 50,000 = 30,000 - anything above trips the cap.
        var reasons = evaluator.evaluate(application, Optional.of(new BigDecimal("30001")));

        assertThat(reasons).containsExactly(RejectionReason.EMI_EXCEEDS_60_PERCENT);
    }

    @Test
    void emiExactlyAtSixtyPercentIsStillAcceptable() {
        LoanApplication application = application(35, new BigDecimal("50000"), 720, 36);

        var reasons = evaluator.evaluate(application, Optional.of(new BigDecimal("30000")));

        assertThat(reasons).isEmpty();
    }

    @Test
    void multipleViolationsSurfaceTogether() {
        // 62-year-old wanting 60 months (=> 67 at maturity), tight income.
        LoanApplication application = application(62, new BigDecimal("20000"), 720, 60);

        var reasons = evaluator.evaluate(application, Optional.of(new BigDecimal("15000")));

        assertThat(reasons).containsExactlyInAnyOrder(
                RejectionReason.AGE_TENURE_LIMIT_EXCEEDED,
                RejectionReason.EMI_EXCEEDS_60_PERCENT
        );
    }

    @Test
    void returnedListIsUnmodifiable() {
        var reasons = evaluator.evaluate(application(35, new BigDecimal("100000"), 720, 36), Optional.empty());
        assertThat(reasons).isUnmodifiable();
    }

    private LoanApplication application(int age, BigDecimal monthlyIncome, int creditScore, int tenureMonths) {
        Applicant applicant = new Applicant("Test", age, monthlyIncome, EmploymentType.SALARIED, creditScore);
        LoanRequest loan = new LoanRequest(new BigDecimal("500000"), tenureMonths, LoanPurpose.PERSONAL);
        return new LoanApplication(applicant, loan);
    }
}
