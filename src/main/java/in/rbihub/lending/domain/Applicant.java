package in.rbihub.lending.domain;

import java.math.BigDecimal;
import java.util.Objects;

public record Applicant(
        String name,
        int age,
        BigDecimal monthlyIncome,
        EmploymentType employmentType,
        int creditScore
) {
    public Applicant {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(monthlyIncome, "monthlyIncome");
        Objects.requireNonNull(employmentType, "employmentType");
    }
}
