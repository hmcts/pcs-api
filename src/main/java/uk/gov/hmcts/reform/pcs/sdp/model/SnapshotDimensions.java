package uk.gov.hmcts.reform.pcs.sdp.model;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class SnapshotDimensions {

    private String claimType;
    private String claimSubType;
    private String language;
    private String tenancyType;
    private String submittedType;
    private String caseManagementLocationCode;
    private List<SnapshotPropertyAddress> propertyAddress;
}
