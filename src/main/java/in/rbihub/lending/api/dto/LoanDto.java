package in.rbihub.lending.api.dto;

import in.rbihub.lending.domain.LoanPurpose;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record LoanDto(

        @NotNull
        @DecimalMin(value = "10000", message = "amount must be at least 10,000")
        @DecimalMax(value = "5000000", message = "amount must be at most 50,00,000")
        BigDecimal amount,

        @NotNull
        @Min(value = 6, message = "tenureMonths must be at least 6")
        @Max(value = 360, message = "tenureMonths must be at most 360")
        Integer tenureMonths,

        @NotNull(message = "purpose is required (PERSONAL, HOME or AUTO)")
        LoanPurpose purpose
) {
}
