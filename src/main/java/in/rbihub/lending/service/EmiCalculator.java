package in.rbihub.lending.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;


@Component
public class EmiCalculator {

    private static final Logger log = LoggerFactory.getLogger(EmiCalculator.class);

    private static final BigDecimal MONTHS_PER_YEAR = BigDecimal.valueOf(12);
    private static final BigDecimal PERCENT = BigDecimal.valueOf(100);

    /** 20 sig figs is overkill for retail loans but cheap and avoids any drift on long tenures. */
    private static final MathContext WORKING = new MathContext(20, RoundingMode.HALF_UP);

    private static final int MONEY_SCALE = 2;
    private static final RoundingMode MONEY_ROUNDING = RoundingMode.HALF_UP;

    public BigDecimal monthlyInstalment(BigDecimal principal,
                                         BigDecimal annualRatePercent,
                                         int tenureMonths) {
        if (principal == null || principal.signum() <= 0) {
            throw new IllegalArgumentException("principal must be positive");
        }
        if (annualRatePercent == null || annualRatePercent.signum() < 0) {
            throw new IllegalArgumentException("annual rate cannot be negative");
        }
        if (tenureMonths <= 0) {
            throw new IllegalArgumentException("tenure must be positive");
        }

        // 0% degenerates to flat principal/n. The formula has 0/0 here, so short-circuit.
        if (annualRatePercent.signum() == 0) {
            return principal.divide(BigDecimal.valueOf(tenureMonths), MONEY_SCALE, MONEY_ROUNDING);
        }

        BigDecimal monthlyRate = annualRatePercent.divide(PERCENT.multiply(MONTHS_PER_YEAR), WORKING);
        BigDecimal compounded = BigDecimal.ONE.add(monthlyRate).pow(tenureMonths, WORKING);
        BigDecimal numerator = principal.multiply(monthlyRate, WORKING).multiply(compounded, WORKING);
        BigDecimal denominator = compounded.subtract(BigDecimal.ONE, WORKING);
        BigDecimal emi = numerator.divide(denominator, MONEY_SCALE, MONEY_ROUNDING);

        log.debug("EMI: P={} annualRate={}% n={} -> monthlyRate={} (1+r)^n={} EMI={}",
                principal, annualRatePercent, tenureMonths, monthlyRate, compounded, emi);
        return emi;
    }

    public BigDecimal totalPayable(BigDecimal monthlyInstalment, int tenureMonths) {
        if (tenureMonths <= 0) {
            throw new IllegalArgumentException("tenure must be positive");
        }
        return monthlyInstalment
                .multiply(BigDecimal.valueOf(tenureMonths))
                .setScale(MONEY_SCALE, MONEY_ROUNDING);
    }
}
