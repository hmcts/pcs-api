package uk.gov.hmcts.reform.pcs.ccd.domain.enforcement;

import lombok.Builder;
import lombok.Data;
import uk.gov.hmcts.ccd.sdk.api.CCD;

/**
 * Encapsulates free-text details for selected enforcement risk categories.
 */
@Builder
@Data
public class EnforcementRiskDetails {

    @CCD(
        label = "How have they been violent or aggressive?",
        hint = "For example, include the crime reference number if you have called police to the property or "
            + "reported an incident. You can enter up to 6,800 characters.",
        typeOverride = uk.gov.hmcts.ccd.sdk.type.FieldType.TextArea
    )
    private String enforcementViolentDetails;

    @CCD(
        label = "What is their history of firearm possession?",
        hint = "For example, include the crime reference number if you have called police to the property or "
            + "reported an incident. You can enter up to 6,800 characters.",
        typeOverride = uk.gov.hmcts.ccd.sdk.type.FieldType.TextArea
    )
    private String enforcementFirearmsDetails;

    @CCD(
        label = "What is their history of criminal or antisocial behaviour?",
        hint = "For example, include the crime reference number if you have called police to the property or "
            + "reported an incident. You can enter up to 6,800 characters.",
        typeOverride = uk.gov.hmcts.ccd.sdk.type.FieldType.TextArea
    )
    private String enforcementCriminalDetails;
}


