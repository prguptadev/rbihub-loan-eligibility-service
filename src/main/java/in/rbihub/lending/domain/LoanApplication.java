package in.rbihub.lending.domain;

import java.util.Objects;

public record LoanApplication(
        Applicant applicant,
        LoanRequest loan
) {
    public LoanApplication {
        Objects.requireNonNull(applicant, "applicant");
        Objects.requireNonNull(loan, "loan");
    }
}
