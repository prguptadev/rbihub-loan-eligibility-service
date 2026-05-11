package in.rbihub.lending.domain;

import java.math.BigDecimal;
import java.util.Objects;

public record Offer(
        BigDecimal interestRate,
        int tenureMonths,
        BigDecimal emi,
        BigDecimal totalPayable
) {
    public Offer {
        Objects.requireNonNull(interestRate, "interestRate");
        Objects.requireNonNull(emi, "emi");
        Objects.requireNonNull(totalPayable, "totalPayable");
    }
}
