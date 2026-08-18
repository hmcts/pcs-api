package uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.ccd.sdk.type.FieldType;
import uk.gov.hmcts.ccd.sdk.type.Flags;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.CitizenAccess;
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

    /**
     * Reasonable adjustment flags from cui-ra microsite, persisted against their party on submit.
     * Every draft save must carry this field. The respond draft save fully replaces the
     * stored draft rather than merging into it.
     */
    @CCD(access = {DefendantAccess.class}, label = "Defendant flags")
    private Flags defendantFlags;

    @CCD(access = {DefendantAccess.class})
    private String currentDefendantPartyId;

    @CCD(access = {DefendantAccess.class})
    private String responseDocumentId;

    @CCD(access = {CitizenAccess.class}, label = "Date issued")
    private LocalDate claimIssuedDate;

}

