package uk.gov.hmcts.reform.pcs.ccd.domain;

import lombok.Builder;
import lombok.Data;
import uk.gov.hmcts.ccd.sdk.api.CCD;

import static uk.gov.hmcts.ccd.sdk.type.FieldType.TextArea;

@Builder
@Data
public class ClaimantCircumstances {

    @CCD(
        label = "Is there any information you'd like to provide about ${claimantNamePossessiveForm} circumstances?",
        hint = "This can be any information about your financial or general situation that you'd "
            + "like the court to consider when making its decision whether or not to grant a possession order"
    )
    private VerticalYesNo claimantCircumstancesSelect;

    @CCD(
        max = 950,
        typeOverride = TextArea
    )
    private String claimantCircumstancesDetails;

    @CCD(searchable = false)
    private String claimantNamePossessiveForm;

}
