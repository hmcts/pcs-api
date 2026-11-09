package uk.gov.hmcts.reform.pcs.sdp.model;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class ClaimSnapshotRecord {

    private String contractVersion;
    private Producer producer;
    private String extractDate;
    private String snapshotId;
    private String publishedDate;
    private ClaimSnapshot claim;
}
