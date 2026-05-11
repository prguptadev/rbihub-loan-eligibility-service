package in.rbihub.lending.service;

import in.rbihub.lending.domain.Decision;
import in.rbihub.lending.domain.LoanApplication;
import in.rbihub.lending.domain.Offer;
import in.rbihub.lending.domain.RejectionReason;
import in.rbihub.lending.domain.RiskBand;
import in.rbihub.lending.config.LendingProperties;
import in.rbihub.lending.repository.DecisionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;


@Service
public class LoanApplicationService {

    private static final Logger log = LoggerFactory.getLogger(LoanApplicationService.class);

    private final RiskBandClassifier riskBandClassifier;
    private final InterestRateCalculator interestRateCalculator;
    private final EmiCalculator emiCalculator;
    private final EligibilityEvaluator eligibilityEvaluator;
    private final DecisionRepository decisionRepository;
    private final LendingProperties policy;

    public LoanApplicationService(RiskBandClassifier riskBandClassifier,
                                   InterestRateCalculator interestRateCalculator,
                                   EmiCalculator emiCalculator,
                                   EligibilityEvaluator eligibilityEvaluator,
                                   DecisionRepository decisionRepository,
                                   LendingProperties policy) {
        this.riskBandClassifier = riskBandClassifier;
        this.interestRateCalculator = interestRateCalculator;
        this.emiCalculator = emiCalculator;
        this.eligibilityEvaluator = eligibilityEvaluator;
        this.decisionRepository = decisionRepository;
        this.policy = policy;
    }

    public Decision evaluate(LoanApplication application) {
        UUID applicationId = UUID.randomUUID();

        log.info("Evaluating application id={} age={} score={} employment={} amount={} tenureMonths={} purpose={}",
                applicationId,
                application.applicant().age(),
                application.applicant().creditScore(),
                application.applicant().employmentType(),
                application.loan().amount(),
                application.loan().tenureMonths(),
                application.loan().purpose());

        Optional<FinancialProjection> projection = project(application);

        List<RejectionReason> hardFailures = eligibilityEvaluator.evaluate(
                application,
                projection.map(FinancialProjection::emi)
        );
        if (!hardFailures.isEmpty()) {
            log.info("Decision id={} status=REJECTED reasons={}", applicationId, hardFailures);
            return persist(new Decision.Rejected(applicationId, hardFailures));
        }

        // Past the eligibility gate, projection is guaranteed present - sub-600
        // scores can't reach this point without tripping CREDIT_SCORE_BELOW_MINIMUM.
        FinancialProjection p = projection.orElseThrow();

        BigDecimal offerCap = application.applicant().monthlyIncome().multiply(policy.emiToIncomeOfferCap());
        if (p.emi().compareTo(offerCap) > 0) {
            log.info("Decision id={} status=REJECTED reasons=[EMI_EXCEEDS_50_PERCENT] emi={} offerCap={}",
                    applicationId, p.emi(), offerCap);
            return persist(new Decision.Rejected(
                    applicationId,
                    List.of(RejectionReason.EMI_EXCEEDS_50_PERCENT)
            ));
        }

        Offer offer = new Offer(
                p.annualRate(),
                application.loan().tenureMonths(),
                p.emi(),
                emiCalculator.totalPayable(p.emi(), application.loan().tenureMonths())
        );
        log.info("Decision id={} status=APPROVED riskBand={} rate={} emi={} totalPayable={}",
                applicationId, p.band(), offer.interestRate(), offer.emi(), offer.totalPayable());
        return persist(new Decision.Approved(applicationId, p.band(), offer));
    }

    private Optional<FinancialProjection> project(LoanApplication application) {
        return riskBandClassifier.classify(application.applicant().creditScore())
                .map(band -> {
                    BigDecimal rate = interestRateCalculator.finalAnnualRate(application, band);
                    BigDecimal emi = emiCalculator.monthlyInstalment(
                            application.loan().amount(),
                            rate,
                            application.loan().tenureMonths()
                    );
                    return new FinancialProjection(band, rate, emi);
                });
    }

    private Decision persist(Decision decision) {
        decisionRepository.save(decision);
        return decision;
    }

    private record FinancialProjection(RiskBand band, BigDecimal annualRate, BigDecimal emi) {
    }
}
