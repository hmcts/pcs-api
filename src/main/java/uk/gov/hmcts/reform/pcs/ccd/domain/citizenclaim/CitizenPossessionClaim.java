package uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.HasDraftVersion;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;

import java.util.List;

@Builder(toBuilder = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CitizenPossessionClaim implements HasDraftVersion {

    private AddressUK propertyAddress;

    private CitizenClaimEligibility eligibility;

    private CitizenClaimClaimant claimant;

    private List<ListValue<CitizenClaimDefendant>> defendants;

    private VerticalYesNo additionalDefendants;

    // Draft only: the frontend's record of the claimant confirming there are no more defendants
    private VerticalYesNo defendantsConfirmed;

    private CitizenClaimTenancy tenancy;

    private CitizenClaimGrounds grounds;

    private CitizenClaimNotice notice;

    private CitizenClaimRentArrears rentArrears;

    private CitizenClaimCircumstances circumstances;

    private CitizenClaimOtherInformation otherInformation;

    private CitizenClaimSubmission submission;

    // Version of the stored draft these answers were read from; a save or submit that echoes it is bound to it.
    private Long draftVersion;

}
