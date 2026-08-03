package uk.gov.hmcts.reform.pcs.ccd.domain.grounds;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;

import static uk.gov.hmcts.ccd.sdk.type.FieldType.TextArea;
import static uk.gov.hmcts.reform.pcs.ccd.domain.constants.ReasonForPossessionHintText.REASON_FOR_POSSESSION_HINT_TEXT;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class IntroductoryDemotedOtherGroundReason {

    @CCD(
        label = "Give details about your reasons for claiming possession (Antisocial behaviour)",
        hint = REASON_FOR_POSSESSION_HINT_TEXT,
        typeOverride = TextArea
    )
    private String antiSocialBehaviourGround;

    @CCD(
        label = "Give details about your reasons for claiming possession (Breach of the tenancy)",
        hint = REASON_FOR_POSSESSION_HINT_TEXT,
        typeOverride = TextArea
    )
    private String breachOfTheTenancyGround;


    @CCD(
        label = "Give details about your reasons for claiming possession (Absolute grounds)",
        hint = REASON_FOR_POSSESSION_HINT_TEXT,
        typeOverride = TextArea
    )
    private String absoluteGrounds;

    @CCD(
        label = "Give details about your reasons for claiming possession (Other grounds)",
        hint = REASON_FOR_POSSESSION_HINT_TEXT,
        typeOverride = TextArea
    )
    private String otherGround;

    @CCD(
        label = "Give details about your reasons for claiming possession (No grounds)",
        hint = REASON_FOR_POSSESSION_HINT_TEXT,
        typeOverride = TextArea
    )
    private String noGrounds;
}
