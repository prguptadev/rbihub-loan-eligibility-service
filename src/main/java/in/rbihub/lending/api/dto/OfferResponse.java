package in.rbihub.lending.api.dto;

import java.math.BigDecimal;

public record OfferResponse(
        BigDecimal interestRate,
        int tenureMonths,
        BigDecimal emi,
        BigDecimal totalPayable
) {
}
