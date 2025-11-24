package uk.gov.hmcts.reform.pcs.ccd.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.ccd.sdk.External;
import uk.gov.hmcts.ccd.sdk.type.FieldType;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.CitizenAccess;

import static uk.gov.hmcts.ccd.sdk.type.FieldType.TextArea;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClaimantInformation {

    @CCD(
        label = "Claimant Name",
        access = {CitizenAccess.class}
    )
    @External
    private String claimantName;

    @CCD(
        label = "Organisation Name"
    )
    @External
    private String organisationName;

    @CCD(
        label = """
            ---
            <p class="govuk-body" tabindex="0">
                Your claimant name registered with My HMCTS is:
            </p>
            """,
        typeOverride = FieldType.Label
    )
    private String organisationNameLabel;

    @CCD(
        label = "Is this the correct claimant name?",
        access = {CitizenAccess.class}
    )
    private VerticalYesNo isClaimantNameCorrect;

    @CCD(
        label = "What is the correct claimant name?",
        hint = """
            Changing your claimant name here only updates it for this claim.
            It does not change your registered claimant name on My HMCTS
            """,
        access = {CitizenAccess.class},
        typeOverride = TextArea
    )
    private String overriddenClaimantName;
}
