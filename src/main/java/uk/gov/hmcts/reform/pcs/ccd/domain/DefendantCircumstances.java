package uk.gov.hmcts.reform.pcs.ccd.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.ccd.sdk.type.FieldType;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.CitizenAccess;

import static uk.gov.hmcts.ccd.sdk.type.FieldType.TextArea;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DefendantCircumstances {

    @CCD(label = """
        ---
        <p class="govuk-body" tabindex="0">
         You can use this section to tell us anything relevant about the ${defendantTermPossessive}
         financial or personal situation.
        </p>
        """,typeOverride = FieldType.Label
    )
    private String defendantCircumstancesLabel;

    @CCD(
        label = "Is there any information you'd like to provide about the ${defendantTermPossessive} circumstances?",
        hint = "This can be any known details or any attempts made to obtain details",
        access = {CitizenAccess.class}
    )
    private VerticalYesNo hasDefendantCircumstancesInfo;

    @CCD(
        label = "Give details about the ${defendantTermPossessive} circumstances",
        typeOverride = TextArea,
        max = 950,
        access = {CitizenAccess.class}
    )
    private String defendantCircumstancesInfo;

    @CCD(access = {CitizenAccess.class})
    private String defendantTermPossessive;
}
