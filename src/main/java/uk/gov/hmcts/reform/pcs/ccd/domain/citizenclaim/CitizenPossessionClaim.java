package uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.HasDraftVersion;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.IntroductoryDemotedOrOtherGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.PrivateLandlordAssuredGround;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;

import java.util.List;
import java.util.Set;

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

    // The claimants claiming alongside the claimant; kept only when claimant.multipleClaimants is YES
    private List<ListValue<CitizenClaimAdditionalClaimant>> additionalClaimants;

    // Draft only: the frontend's record of the claimant confirming there are no more defendants
    private VerticalYesNo defendantsConfirmed;

    private CitizenClaimTenancy tenancy;

    private CitizenClaimGrounds grounds;

    private CitizenClaimNotice notice;

    private CitizenClaimRentArrears rentArrears;

    private CitizenClaimCircumstances circumstances;

    private CitizenClaimOtherInformation otherInformation;

    private CitizenClaimSubmission submission;

    // Draft only: the sections whose check your answers page the claimant has continued from
    private Set<CitizenClaimSection> confirmedSections;

    // Version of the stored draft these answers were read from; a save or submit that echoes it is bound to it.
    private Long draftVersion;

    /**
     * Whether the claim is for a property in Wales, which asks about the occupation contract instead of the tenancy.
     */
    @JsonIgnore
    public boolean isWales() {
        return eligibility != null && eligibility.getLegislativeCountry() == LegislativeCountry.WALES;
    }

    /**
     * Whether a claim on an excluded tenancy in England, which asks its own grounds questions.
     */
    @JsonIgnore
    public boolean isExcludedTenancy() {
        return !isWales()
            && tenancy != null
            && tenancy.getTenancyType() == CitizenClaimTenancyType.EXCLUDED;
    }

    public boolean hasRentArrearsGround() {
        if (isExcludedTenancy()) {
            return grounds != null
                && grounds.getHasExcludedGrounds() == VerticalYesNo.YES
                && grounds.getExcludedGrounds() != null
                && grounds.getExcludedGrounds().contains(IntroductoryDemotedOrOtherGrounds.RENT_ARREARS);
        }
        return grounds != null
            && (grounds.allGrounds().stream().anyMatch(PrivateLandlordAssuredGround::isRentArrears)
                || grounds.hasWalesRentArrearsGround());
    }

}
