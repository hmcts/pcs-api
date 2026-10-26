package uk.gov.hmcts.reform.pcs.sdp.model;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class ClaimSnapshot {

    private String claimReference;
    private int snapshotVersion;
    private String recordedDate;
    private SnapshotDimensions dimensions;
    private SnapshotMilestones milestones;
    private List<SnapshotParty> parties;
    private List<SnapshotPaymentReference> paymentReferences;
    private List<SnapshotGround> grounds;
    private List<SnapshotResponse> responses;
}
