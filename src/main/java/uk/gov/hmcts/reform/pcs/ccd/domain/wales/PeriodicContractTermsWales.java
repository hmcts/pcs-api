package uk.gov.hmcts.reform.pcs.ccd.domain.wales;

import lombok.Builder;
import lombok.Data;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;

import static uk.gov.hmcts.ccd.sdk.type.FieldType.TextArea;

@Data
@Builder
public class PeriodicContractTermsWales {

    @CCD(
        label = "Have you and the contract holder agreed terms of the periodic standard contract "
            + "in addition to those incorporated by statute?"
    )
    private VerticalYesNo agreedTermsOfPeriodicContract;

    @CCD(
        label = "Give details of the terms you’ve agreed",
        hint = "You can enter up to 250 characters",
        typeOverride = TextArea
    )
    private String detailsOfTerms;
}
