package in.rbihub.lending.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import in.rbihub.lending.domain.DecisionStatus;
import in.rbihub.lending.domain.RejectionReason;
import in.rbihub.lending.domain.RiskBand;

import java.util.List;
import java.util.UUID;


public record DecisionResponse(

        UUID applicationId,

        DecisionStatus status,

        RiskBand riskBand,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        OfferResponse offer,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        List<RejectionReason> rejectionReasons
) {
    public static DecisionResponse approved(UUID applicationId, RiskBand riskBand, OfferResponse offer) {
        return new DecisionResponse(applicationId, DecisionStatus.APPROVED, riskBand, offer, null);
    }

    public static DecisionResponse rejected(UUID applicationId, List<RejectionReason> reasons) {
        return new DecisionResponse(applicationId, DecisionStatus.REJECTED, null, null, reasons);
    }
}
