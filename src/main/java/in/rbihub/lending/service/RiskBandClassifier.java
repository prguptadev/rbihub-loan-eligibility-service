package in.rbihub.lending.service;

import in.rbihub.lending.config.LendingProperties;
import in.rbihub.lending.domain.RiskBand;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class RiskBandClassifier {

    private final LendingProperties policy;

    public RiskBandClassifier(LendingProperties policy) {
        this.policy = policy;
    }

    public Optional<RiskBand> classify(int creditScore) {
        if (creditScore >= policy.lowRiskScoreFloor()) {
            return Optional.of(RiskBand.LOW);
        }
        if (creditScore >= policy.mediumRiskScoreFloor()) {
            return Optional.of(RiskBand.MEDIUM);
        }
        if (creditScore >= policy.highRiskScoreFloor()) {
            return Optional.of(RiskBand.HIGH);
        }
        return Optional.empty();
    }
}
