package in.rbihub.lending.domain;

import java.util.List;
import java.util.Objects;
import java.util.UUID;


public sealed interface Decision permits Decision.Approved, Decision.Rejected {

    UUID applicationId();

    DecisionStatus status();

    record Approved(UUID applicationId, RiskBand riskBand, Offer offer) implements Decision {
        public Approved {
            Objects.requireNonNull(applicationId, "applicationId");
            Objects.requireNonNull(riskBand, "riskBand");
            Objects.requireNonNull(offer, "offer");
        }

        @Override
        public DecisionStatus status() {
            return DecisionStatus.APPROVED;
        }
    }

    record Rejected(UUID applicationId, List<RejectionReason> reasons) implements Decision {
        public Rejected {
            Objects.requireNonNull(applicationId, "applicationId");
            Objects.requireNonNull(reasons, "reasons");
            if (reasons.isEmpty()) {
                throw new IllegalArgumentException("rejected decision must carry at least one reason");
            }
            // Defensive copy - the canonical accessor will hand this list out, and
            // we don't want callers mutating audit state by accident.
            reasons = List.copyOf(reasons);
        }

        @Override
        public DecisionStatus status() {
            return DecisionStatus.REJECTED;
        }
    }
}
