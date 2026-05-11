package in.rbihub.lending.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest
@AutoConfigureMockMvc
class ApplicationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void approvedApplicationReturnsOkWithOffer() throws Exception {
        String payload = """
                {
                  "applicant": {
                    "name": "Asha",
                    "age": 30,
                    "monthlyIncome": 75000,
                    "employmentType": "SALARIED",
                    "creditScore": 800
                  },
                  "loan": {
                    "amount": 500000,
                    "tenureMonths": 36,
                    "purpose": "PERSONAL"
                  }
                }
                """;

        mockMvc.perform(post("/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.applicationId").exists())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.riskBand").value("LOW"))
                .andExpect(jsonPath("$.offer.interestRate").value(12.00))
                .andExpect(jsonPath("$.offer.tenureMonths").value(36))
                .andExpect(jsonPath("$.offer.emi").value(16607.15))
                .andExpect(jsonPath("$.offer.totalPayable").value(597857.40))
                .andExpect(jsonPath("$.rejectionReasons").doesNotExist());
    }

    @Test
    void businessRejectionAlsoReturnsOkWithReasons() throws Exception {
        // Credit score below the gate.
        String payload = """
                {
                  "applicant": {
                    "name": "Chitra",
                    "age": 30,
                    "monthlyIncome": 75000,
                    "employmentType": "SALARIED",
                    "creditScore": 580
                  },
                  "loan": {
                    "amount": 500000,
                    "tenureMonths": 36,
                    "purpose": "PERSONAL"
                  }
                }
                """;

        mockMvc.perform(post("/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.applicationId").exists())
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.riskBand").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.offer").doesNotExist())
                .andExpect(jsonPath("$.rejectionReasons[0]").value("CREDIT_SCORE_BELOW_MINIMUM"));
    }

    @Test
    void outOfRangeAgeReturnsBadRequestWithFieldError() throws Exception {
        String payload = """
                {
                  "applicant": {
                    "name": "Too young",
                    "age": 18,
                    "monthlyIncome": 75000,
                    "employmentType": "SALARIED",
                    "creditScore": 800
                  },
                  "loan": {
                    "amount": 500000,
                    "tenureMonths": 36,
                    "purpose": "PERSONAL"
                  }
                }
                """;

        mockMvc.perform(post("/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'applicant.age')]").exists());
    }

    @Test
    void multipleValidationFailuresAreReportedTogether() throws Exception {
        String payload = """
                {
                  "applicant": {
                    "name": "",
                    "age": 70,
                    "monthlyIncome": 0,
                    "employmentType": "SALARIED",
                    "creditScore": 100
                  },
                  "loan": {
                    "amount": 100,
                    "tenureMonths": 1000,
                    "purpose": "PERSONAL"
                  }
                }
                """;

        mockMvc.perform(post("/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'applicant.name')]").exists())
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'applicant.age')]").exists())
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'applicant.monthlyIncome')]").exists())
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'applicant.creditScore')]").exists())
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'loan.amount')]").exists())
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'loan.tenureMonths')]").exists());
    }

    @Test
    void unknownEmploymentTypeReturnsBadRequestWithAllowedValues() throws Exception {
        String payload = """
                {
                  "applicant": {
                    "name": "Asha",
                    "age": 30,
                    "monthlyIncome": 75000,
                    "employmentType": "FREELANCE",
                    "creditScore": 800
                  },
                  "loan": {
                    "amount": 500000,
                    "tenureMonths": 36,
                    "purpose": "PERSONAL"
                  }
                }
                """;

        mockMvc.perform(post("/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("employmentType")))
                .andExpect(jsonPath("$.message").value(containsString("SALARIED")))
                .andExpect(jsonPath("$.message").value(containsString("SELF_EMPLOYED")));
    }

    @Test
    void emptyBodyReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(""))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }
}
