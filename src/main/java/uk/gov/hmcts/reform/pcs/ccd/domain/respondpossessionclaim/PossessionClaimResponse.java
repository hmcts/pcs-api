package uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.ccd.sdk.type.FieldType;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.CitizenAccess;

import java.util.List;

/**
 * Defendant's response to a possession claim.
 *
 * <p><b>IMPORTANT:</b> {@code @Builder(toBuilder = true)} is REQUIRED.
 */
@Builder(toBuilder = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PossessionClaimResponse {

    /**
     * Claimant organisation names visible to defendants.
     * Extracted from allClaimants (filtered to PartyRole.CLAIMANT by PCSCaseView).
     * Supports multiple claimants (e.g., joint landlords).
     */
    @CCD(
        access = {CitizenAccess.class},
        typeOverride = FieldType.Collection,
        typeParameterOverride = "Text"
    )
    private List<ListValue<String>> claimantOrganisations;

    @CCD(access = {CitizenAccess.class})
    private DefendantContactDetails defendantContactDetails;

    @CCD(access = {CitizenAccess.class})
    private DefendantResponses defendantResponses;
}

