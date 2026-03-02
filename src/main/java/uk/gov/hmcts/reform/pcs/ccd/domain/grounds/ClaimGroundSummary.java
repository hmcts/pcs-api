package uk.gov.hmcts.reform.pcs.ccd.domain.grounds;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Builder;
import lombok.Data;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimGroundCategory;

@Builder
@Data
public class ClaimGroundSummary {

    private ClaimGroundCategory category;

    private String code;

    private String label;

    private String description;

    private String reason;

    private YesOrNo isRentArrears;

    @JsonIgnore
    private int categoryRank;

    @JsonIgnore
    private int groundRank;

}
