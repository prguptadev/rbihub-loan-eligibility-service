package in.rbihub.lending.config;

import java.math.BigDecimal;


public final class LendingPropertiesFixtures {

    private LendingPropertiesFixtures() {
    }

    public static LendingProperties standardPolicy() {
        return new LendingProperties(
                new BigDecimal("12.00"),

                // Risk-band score floors
                750,
                650,
                600,

                // Premiums
                new BigDecimal("1.50"),       // medium-risk
                new BigDecimal("3.00"),       // high-risk
                new BigDecimal("1.00"),       // self-employed
                new BigDecimal("1000000"),    // large-loan threshold
                new BigDecimal("0.50"),       // large-loan premium

                // Eligibility
                600,                          // min credit score
                65,                           // max age at maturity (years)
                new BigDecimal("0.60"),       // EMI/income hard cap

                // Offer viability
                new BigDecimal("0.50")        // EMI/income offer cap
        );
    }
}
