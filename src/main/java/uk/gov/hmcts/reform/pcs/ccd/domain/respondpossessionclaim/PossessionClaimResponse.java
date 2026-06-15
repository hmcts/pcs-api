package uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.ccd.sdk.type.FieldType;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.DefendantAccess;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;

import java.time.LocalDate;
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
        access = {DefendantAccess.class},
        typeOverride = FieldType.Collection,
        typeParameterOverride = "Text"
    )
    private List<ListValue<String>> claimantOrganisations;

    @CCD(access = {CitizenAccess.class}, label = "Address for service")
    private AddressUK claimantServiceAddress;

    @CCD(access = {CitizenAccess.class})
    private Party claimantEnteredDefendantDetails;

    @CCD(access = {DefendantAccess.class})
    private DefendantContactDetails defendantContactDetails;

    @CCD(access = {DefendantAccess.class})
    private DefendantResponses defendantResponses;

    @CCD(access = {DefendantAccess.class})
    private String currentDefendantPartyId;

    @CCD(access = {CitizenAccess.class}, label = "Date issued")
    private LocalDate claimIssuedDate;

}

