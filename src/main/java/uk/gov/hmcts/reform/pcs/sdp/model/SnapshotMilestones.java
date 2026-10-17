package uk.gov.hmcts.reform.pcs.sdp.model;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class SnapshotMilestones {

    private String postalClaimReceivedByCourtDate;
    private String claimSubmittedToSystemDate;
    private String claimFeePaidDate;
    private String issueClaimDate;
}
