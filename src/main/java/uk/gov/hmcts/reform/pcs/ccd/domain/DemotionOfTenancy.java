package uk.gov.hmcts.reform.pcs.ccd.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;

import static uk.gov.hmcts.ccd.sdk.type.FieldType.TextArea;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DemotionOfTenancy {

    @CCD(
        label = "Which section of the Housing Act is the claim for demotion of tenancy made under?"
    )
    private DemotionOfTenancyHousingAct demotionOfTenancyHousingActs;

    @CCD(
        label = "Have you served the defendants with a statement of the express terms which will apply "
            + "to the demoted tenancy?"
    )
    private VerticalYesNo statementOfExpressTermsServed;

    @CCD(
        label = "Give details of the terms",
        hint = "You can enter up to 950 characters",
        typeOverride = TextArea
    )
    private String statementOfExpressTermsDetails;

    @CCD(
        label = "Why are you requesting a demotion order?",
        hint = "Give details of the defendants' conduct and any other reasons you think are relevant. "
            + "You can enter up to 250 characters",
        typeOverride = TextArea
    )
    private String demotionOfTenancyReason;

    private YesOrNo showDemotionOfTenancyHousingActsPage;
}
