package uk.gov.hmcts.reform.pcs.sdp.model;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class SnapshotRequestedFee {

    private String feeId;
    private String feeReference;
    private String feeTypeId;
    private String feeTypeDescription;
    private BigDecimal feeAmount;
    private String feeStatus;
    private List<SnapshotFeeParty> feeRequestedOfTheseParties;
    private String feeRequestedDate;
    private String responsiblePartyId;
}
