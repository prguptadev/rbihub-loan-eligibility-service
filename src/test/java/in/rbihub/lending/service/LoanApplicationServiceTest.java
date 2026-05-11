package in.rbihub.lending.service;

import in.rbihub.lending.config.LendingProperties;
import in.rbihub.lending.config.LendingPropertiesFixtures;
import in.rbihub.lending.domain.Applicant;
import in.rbihub.lending.domain.Decision;
import in.rbihub.lending.domain.DecisionStatus;
import in.rbihub.lending.domain.EmploymentType;
import in.rbihub.lending.domain.LoanApplication;
import in.rbihub.lending.domain.LoanPurpose;
import in.rbihub.lending.domain.LoanRequest;
import in.rbihub.lending.domain.RejectionReason;
import in.rbihub.lending.domain.RiskBand;
import in.rbihub.lending.repository.InMemoryDecisionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;


class LoanApplicationServiceTest {

    private InMemoryDecisionRepository repository;
    private LoanApplicationService service;

    @BeforeEach
    void setUp() {
        LendingProperties policy = LendingPropertiesFixtures.standardPolicy();
        repository = new InMemoryDecisionRepository();
        service = new LoanApplicationService(
                new RiskBandClassifier(policy),
                new InterestRateCalculator(policy),
                new EmiCalculator(),
                new EligibilityEvaluator(policy),
                repository,
                policy
        );
    }

    @Test
    void salariedLowRiskApplicationIsApprovedWithComputedOffer() {
        LoanApplication application = new LoanApplication(
                new Applicant("Asha", 30, new BigDecimal("75000"), EmploymentType.SALARIED, 800),
                new LoanRequest(new BigDecimal("500000"), 36, LoanPurpose.PERSONAL)
        );

        Decision decision = service.evaluate(application);

        assertThat(decision).isInstanceOf(Decision.Approved.class);
        Decision.Approved approved = (Decision.Approved) decision;
        assertThat(approved.riskBand()).isEqualTo(RiskBand.LOW);
        assertThat(approved.offer().interestRate()).isEqualByComparingTo("12.00");
        assertThat(approved.offer().tenureMonths()).isEqualTo(36);
        assertThat(approved.offer().emi()).isEqualByComparingTo("16607.15");
        assertThat(approved.offer().totalPayable()).isEqualByComparingTo("597857.40");
    }

    @Test
    void selfEmployedHighRiskLargeLoanGetsAllPremiumsAndStillApprovedIfAffordable() {
        LoanApplication application = new LoanApplication(
                new Applicant("Bharat", 40, new BigDecimal("400000"), EmploymentType.SELF_EMPLOYED, 620),
                new LoanRequest(new BigDecimal("1500000"), 60, LoanPurpose.HOME)
        );

        Decision decision = service.evaluate(application);

        assertThat(decision).isInstanceOf(Decision.Approved.class);
        Decision.Approved approved = (Decision.Approved) decision;
        assertThat(approved.riskBand()).isEqualTo(RiskBand.HIGH);
        // 12 + 3 (HIGH) + 1 (self-employed) + 0.5 (large loan) = 16.50
        assertThat(approved.offer().interestRate()).isEqualByComparingTo("16.50");
    }

    @Test
    void creditScoreUnderSixHundredIsRejectedWithoutComputingOffer() {
        LoanApplication application = new LoanApplication(
                new Applicant("Chitra", 30, new BigDecimal("75000"), EmploymentType.SALARIED, 580),
                new LoanRequest(new BigDecimal("500000"), 36, LoanPurpose.PERSONAL)
        );

        Decision decision = service.evaluate(application);

        assertThat(decision).isInstanceOf(Decision.Rejected.class);
        assertThat(((Decision.Rejected) decision).reasons())
                .containsExactly(RejectionReason.CREDIT_SCORE_BELOW_MINIMUM);
    }

    @Test
    void agePlusTenureExceedsSixtyFiveYearsTriggersRejection() {
        LoanApplication application = new LoanApplication(
                new Applicant("Deepak", 60, new BigDecimal("200000"), EmploymentType.SALARIED, 750),
                new LoanRequest(new BigDecimal("500000"), 84, LoanPurpose.PERSONAL)  // 60 + 7 = 67
        );

        Decision decision = service.evaluate(application);

        assertThat(decision).isInstanceOf(Decision.Rejected.class);
        assertThat(((Decision.Rejected) decision).reasons())
                .contains(RejectionReason.AGE_TENURE_LIMIT_EXCEEDED);
    }

    @Test
    void emiAboveSixtyPercentOfIncomeIsRejectedWithHardCapReason() {
        // 50K loan at 12% over 6 months -> EMI ~8625, way above 60% of 10K income.
        LoanApplication application = new LoanApplication(
                new Applicant("Esha", 30, new BigDecimal("10000"), EmploymentType.SALARIED, 800),
                new LoanRequest(new BigDecimal("50000"), 6, LoanPurpose.PERSONAL)
        );

        Decision decision = service.evaluate(application);

        assertThat(decision).isInstanceOf(Decision.Rejected.class);
        assertThat(((Decision.Rejected) decision).reasons())
                .contains(RejectionReason.EMI_EXCEEDS_60_PERCENT);
    }

    @Test
    void emiBetweenFiftyAndSixtyPercentTripsTheOfferGate() {
        // Tuned so EMI lands in the 50-60% band: 50K loan, 6 months, income 16000.
        // EMI ~8625 -> 53.9% of income, between the two caps.
        LoanApplication application = new LoanApplication(
                new Applicant("Farhan", 30, new BigDecimal("16000"), EmploymentType.SALARIED, 800),
                new LoanRequest(new BigDecimal("50000"), 6, LoanPurpose.PERSONAL)
        );

        Decision decision = service.evaluate(application);

        assertThat(decision).isInstanceOf(Decision.Rejected.class);
        assertThat(((Decision.Rejected) decision).reasons())
                .containsExactly(RejectionReason.EMI_EXCEEDS_50_PERCENT);
    }

    @Test
    void multipleEligibilityFailuresSurfaceTogether() {
        // Old applicant, long tenure, tight income -> age/tenure + EMI both trip.
        LoanApplication application = new LoanApplication(
                new Applicant("Gita", 60, new BigDecimal("15000"), EmploymentType.SALARIED, 720),
                new LoanRequest(new BigDecimal("500000"), 84, LoanPurpose.PERSONAL)
        );

        Decision decision = service.evaluate(application);

        assertThat(decision).isInstanceOf(Decision.Rejected.class);
        assertThat(((Decision.Rejected) decision).reasons())
                .containsExactlyInAnyOrder(
                        RejectionReason.AGE_TENURE_LIMIT_EXCEEDED,
                        RejectionReason.EMI_EXCEEDS_60_PERCENT
                );
    }

    @Test
    void everyDecisionGetsPersistedToTheAuditStore() {
        LoanApplication application = new LoanApplication(
                new Applicant("Asha", 30, new BigDecimal("75000"), EmploymentType.SALARIED, 800),
                new LoanRequest(new BigDecimal("500000"), 36, LoanPurpose.PERSONAL)
        );

        Decision decision = service.evaluate(application);

        assertThat(repository.findById(decision.applicationId())).contains(decision);
    }

    @Test
    void distinctApplicationsGetDistinctIds() {
        LoanApplication application = new LoanApplication(
                new Applicant("Asha", 30, new BigDecimal("75000"), EmploymentType.SALARIED, 800),
                new LoanRequest(new BigDecimal("500000"), 36, LoanPurpose.PERSONAL)
        );

        Decision first = service.evaluate(application);
        Decision second = service.evaluate(application);

        assertThat(first.applicationId()).isNotEqualTo(second.applicationId());
    }

    @Test
    void approvedDecisionStatusReportsApproved() {
        LoanApplication application = new LoanApplication(
                new Applicant("Asha", 30, new BigDecimal("75000"), EmploymentType.SALARIED, 800),
                new LoanRequest(new BigDecimal("500000"), 36, LoanPurpose.PERSONAL)
        );

        Decision decision = service.evaluate(application);

        assertThat(decision.status()).isEqualTo(DecisionStatus.APPROVED);
    }
}
