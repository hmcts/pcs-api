package uk.gov.hmcts.reform.pcs.ccd.domain.grounds;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;

import java.util.Set;

import static uk.gov.hmcts.ccd.sdk.type.FieldType.MultiSelectList;
import static uk.gov.hmcts.ccd.sdk.type.FieldType.TextArea;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
public class AssuredNoArrearsPossessionGrounds {

    @CCD(
        label = "Mandatory grounds",
        hint = "Select all that you allege apply",
        typeOverride = MultiSelectList,
        typeParameterOverride = "AssuredMandatoryGround"
    )
    private Set<AssuredMandatoryGround> mandatoryGrounds;

    @CCD(
        label = "Discretionary grounds",
        hint = "Select all that you allege apply",
        typeOverride = MultiSelectList,
        typeParameterOverride = "AssuredDiscretionaryGround"
    )
    private Set<AssuredDiscretionaryGround> discretionaryGrounds;
    private YesOrNo showGroundReasonPage;

    @CCD(
            label = "Additional grounds",
            hint = "Select all that you allege apply",
            typeOverride = MultiSelectList,
            typeParameterOverride = "AssuredAdditionalOtherGround"
    )
    private Set<AssuredAdditionalOtherGround> otherGround;

    @CCD(
            label = "Give details of your other grounds for possession",
            typeOverride = TextArea
    )
    private String otherGroundDescription;
}

