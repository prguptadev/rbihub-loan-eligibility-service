package in.rbihub.lending.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record LoanApplicationRequest(

        @NotNull
        @Valid
        ApplicantDto applicant,

        @NotNull
        @Valid
        LoanDto loan
) {
}
