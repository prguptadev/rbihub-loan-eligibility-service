package in.rbihub.lending.domain;

import java.math.BigDecimal;
import java.util.Objects;

public record LoanRequest(
        BigDecimal amount,
        int tenureMonths,
        LoanPurpose purpose
) {
    public LoanRequest {
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(purpose, "purpose");
    }
}
