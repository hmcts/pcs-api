package uk.gov.hmcts.reform.pcs.ccd.domain;


import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.CaseworkerAccess;

import static uk.gov.hmcts.ccd.sdk.type.FieldType.TextArea;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class IntroductoryDemotedOtherGroundReason {

    @CCD(
        label = "Give details about your reason for possession",
        hint = "You'll be able to upload documents to support or further explain your reasons later on",
        typeOverride = TextArea,
        access = { CaseworkerAccess.class }
    )
    private String antiSocialBehaviourGround;

    @CCD(
        label = "Give details about your reason for possession",
        hint = "You'll be able to upload documents to support or further explain your reasons later on",
        typeOverride = TextArea,
        access = { CaseworkerAccess.class }
    )
    private String breachOfTenancyGround;


    @CCD(
        label = "Give details about your reason for possession",
        hint = "You'll be able to upload documents to support or further explain your reasons later on",
        typeOverride = TextArea,
        access = { CaseworkerAccess.class }
    )
    private String absoluteGrounds;

    @CCD(
        label = "Give details about your reason for possession",
        hint = "You'll be able to upload documents to support or further explain your reasons later on",
        typeOverride = TextArea,
        access = { CaseworkerAccess.class }
    )
    private String otherGround;

}
