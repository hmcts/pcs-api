package uk.gov.hmcts.reform.pcs.ccd.domain;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;

import static uk.gov.hmcts.ccd.sdk.type.FieldType.TextArea;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class IntroductoryDemotedOtherGroundReason {

    @CCD(
        label = "Give details about your reason for possession (Antisocial behaviour)",
        hint = "You'll be able to upload documents to support or further explain your reasons later on",
        typeOverride = TextArea,
        max = 500
    )
    private String antiSocialBehaviourGround;

    @CCD(
        label = "Give details about your reason for possession (Breach of the tenancy)",
        hint = "You'll be able to upload documents to support or further explain your reasons later on",
        typeOverride = TextArea,
        max = 500
    )
    private String breachOfTenancyGround;


    @CCD(
        label = "Give details about your reason for possession (Absolute grounds)",
        hint = "You'll be able to upload documents to support or further explain your reasons later on",
        typeOverride = TextArea,
        max = 500
    )
    private String absoluteGrounds;

    @CCD(
        label = "Give details about your reason for possession (Other grounds)",
        hint = "You'll be able to upload documents to support or further explain your reasons later on",
        typeOverride = TextArea,
        max = 500
    )
    private String otherGround;

}
