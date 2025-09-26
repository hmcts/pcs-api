package uk.gov.hmcts.reform.pcs.ccd.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.CitizenAccess;

import static uk.gov.hmcts.ccd.sdk.type.FieldType.TextArea;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DefendantCircumstances {

    @CCD(
        label = "Is there any information you'd like to provide about the ${dynamicDefendantText} circumstances?",
        hint = "This can be any known details or any attempts made to obtain details ",
        access = {CitizenAccess.class}
    )
    private VerticalYesNo hasDefendantCircumstancesInfo;

    @CCD(
        label = "Give details about the ${dynamicDefendantText} circumstances",
        typeOverride = TextArea,
        max = 950,
        access = {CitizenAccess.class}
    )
    private String defendantCircumstancesInfo;

}
