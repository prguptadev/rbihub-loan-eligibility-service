package in.rbihub.lending.api;

import in.rbihub.lending.api.dto.DecisionResponse;
import in.rbihub.lending.api.dto.LoanApplicationRequest;
import in.rbihub.lending.api.dto.OfferResponse;
import in.rbihub.lending.domain.Applicant;
import in.rbihub.lending.domain.Decision;
import in.rbihub.lending.domain.LoanApplication;
import in.rbihub.lending.domain.LoanRequest;
import org.springframework.stereotype.Component;


@Component
public class ApplicationDtoMapper {

    public LoanApplication toDomain(LoanApplicationRequest request) {
        Applicant applicant = new Applicant(
                request.applicant().name(),
                request.applicant().age(),
                request.applicant().monthlyIncome(),
                request.applicant().employmentType(),
                request.applicant().creditScore()
        );
        LoanRequest loan = new LoanRequest(
                request.loan().amount(),
                request.loan().tenureMonths(),
                request.loan().purpose()
        );
        return new LoanApplication(applicant, loan);
    }

    public DecisionResponse toResponse(Decision decision) {
        return switch (decision) {
            case Decision.Approved approved -> DecisionResponse.approved(
                    approved.applicationId(),
                    approved.riskBand(),
                    new OfferResponse(
                            approved.offer().interestRate(),
                            approved.offer().tenureMonths(),
                            approved.offer().emi(),
                            approved.offer().totalPayable()
                    )
            );
            case Decision.Rejected rejected -> DecisionResponse.rejected(
                    rejected.applicationId(),
                    rejected.reasons()
            );
        };
    }
}
