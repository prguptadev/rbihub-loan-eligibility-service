package in.rbihub.lending.api.dto;

import in.rbihub.lending.domain.EmploymentType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record ApplicantDto(

        @NotBlank
        String name,

        @NotNull
        @Min(value = 21, message = "age must be at least 21")
        @Max(value = 60, message = "age must be at most 60")
        Integer age,

        @NotNull
        @Positive(message = "monthlyIncome must be greater than 0")
        BigDecimal monthlyIncome,

        @NotNull(message = "employmentType is required (SALARIED or SELF_EMPLOYED)")
        EmploymentType employmentType,

        @NotNull
        @Min(value = 300, message = "creditScore must be at least 300")
        @Max(value = 900, message = "creditScore must be at most 900")
        Integer creditScore
) {
}
