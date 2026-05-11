package in.rbihub.lending.service;

import in.rbihub.lending.config.LendingPropertiesFixtures;
import in.rbihub.lending.domain.RiskBand;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class RiskBandClassifierTest {

    private final RiskBandClassifier classifier = new RiskBandClassifier(LendingPropertiesFixtures.standardPolicy());

    @ParameterizedTest(name = "score {0} -> {1}")
    @CsvSource({
            // Boundaries first - that's where rules tend to break.
            "900, LOW",
            "750, LOW",
            "749, MEDIUM",
            "650, MEDIUM",
            "649, HIGH",
            "600, HIGH"
    })
    void classifiesScoresIntoExpectedBands(int score, RiskBand expected) {
        assertThat(classifier.classify(score)).contains(expected);
    }

    @ParameterizedTest
    @CsvSource({"599", "500", "300"})
    void scoresBelowSixHundredHaveNoRiskBand(int score) {
        assertThat(classifier.classify(score)).isEmpty();
    }

    @Test
    void wellAboveLowFloorStillLow() {
        assertThat(classifier.classify(820)).contains(RiskBand.LOW);
    }
}
