package in.rbihub.lending.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EmiCalculatorTest {

    private final EmiCalculator emiCalculator = new EmiCalculator();

    @Test
    @DisplayName("classic case: 5L principal, 12% p.a., 36 months")
    void emiAtBaseRateForThreeYears() {
        BigDecimal emi = emiCalculator.monthlyInstalment(
                new BigDecimal("500000"),
                new BigDecimal("12"),
                36
        );

        // Cross-checked against the standard reducing-balance formula.
        assertThat(emi).isEqualByComparingTo("16607.15");
    }

    /**
     * Spot-checks against figures lenders typically publish for round-number
     * loans. Tolerances are tight - the BigDecimal formula should land on the
     * paisa, not just close to it.
     */
    @ParameterizedTest(name = "{0} @ {1}% for {2}m -> {3}")
    @CsvSource({
            "100000,  12, 12,  8884.88",
            "500000,  12, 60, 11122.22",
            "1000000, 10, 120, 13215.07",
            "250000, 13.5, 24, 11944.25"
    })
    void emiMatchesPublishedFigures(String principal,
                                     String annualRate,
                                     int tenureMonths,
                                     String expectedEmi) {
        BigDecimal emi = emiCalculator.monthlyInstalment(
                new BigDecimal(principal),
                new BigDecimal(annualRate),
                tenureMonths
        );
        assertThat(emi).isEqualByComparingTo(expectedEmi);
    }

    @Test
    @DisplayName("zero interest degenerates to flat principal / tenure")
    void zeroInterestRateProducesFlatInstalment() {
        BigDecimal emi = emiCalculator.monthlyInstalment(
                new BigDecimal("120000"),
                BigDecimal.ZERO,
                12
        );
        assertThat(emi).isEqualByComparingTo("10000.00");
    }

    @Test
    void resultAlwaysCarriesScaleOfTwo() {
        BigDecimal emi = emiCalculator.monthlyInstalment(
                new BigDecimal("123456"),
                new BigDecimal("11.25"),
                47
        );
        assertThat(emi.scale()).isEqualTo(2);
    }

    @Test
    void totalPayableMultipliesByTenureAndKeepsTwoDecimals() {
        BigDecimal total = emiCalculator.totalPayable(new BigDecimal("16607.15"), 36);
        assertThat(total).isEqualByComparingTo("597857.40");
        assertThat(total.scale()).isEqualTo(2);
    }

    @Test
    void rejectsNonPositivePrincipal() {
        assertThatThrownBy(() ->
                emiCalculator.monthlyInstalment(BigDecimal.ZERO, new BigDecimal("12"), 12))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNegativeAnnualRate() {
        assertThatThrownBy(() ->
                emiCalculator.monthlyInstalment(new BigDecimal("100000"), new BigDecimal("-1"), 12))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNonPositiveTenure() {
        assertThatThrownBy(() ->
                emiCalculator.monthlyInstalment(new BigDecimal("100000"), new BigDecimal("12"), 0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
