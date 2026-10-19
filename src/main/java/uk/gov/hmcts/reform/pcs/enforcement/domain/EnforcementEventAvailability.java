package uk.gov.hmcts.reform.pcs.enforcement.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.InternalTabAccess;
import uk.gov.hmcts.reform.pcs.enforcement.accesscontrol.ClaimantEventFlagAccess;

/**
 * Which enforcement events the claim's current enforcement allows, set for the case view by
 * {@link uk.gov.hmcts.reform.pcs.enforcement.view.EnforcementEventAvailabilityView}. Each event's show condition
 * reads its flag, so an event the start callback would refuse is not offered in the next step drop down. The flags
 * come from the same rules the start callbacks check, which stay as the backstop. Read only and never shown: no
 * event edits them and no tab displays them.
 *
 * <p>"Schedule eviction", "Reschedule eviction" and "Mark notice of eviction as served" read the flags on
 * {@link EvictionSchedule}, and "Confirm the eviction details" reads
 * {@link EnforcementOrder#getShowConfirmEvictionJourney()}. "Record warrant return" has no flag: HDPI-7347
 * asks for it to be offered whatever the claim's enforcement is, and refused when it starts.</p>
 */
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EnforcementEventAvailability {

    /**
     * Gates "Enforce the order" and "Resume draft application": the claim has no live enforcement.
     */
    @CCD(searchable = false, access = {ClaimantEventFlagAccess.class})
    private YesOrNo canApplyForEnforcement;

    /**
     * Gates "Decide enforcement": an application is awaiting review.
     */
    @CCD(searchable = false, access = {InternalTabAccess.class})
    private YesOrNo canDecideEnforcement;

    /**
     * Gates "Allocate case to bailiff": the current enforcement is an approved, live warrant.
     */
    @CCD(searchable = false, access = {InternalTabAccess.class})
    private YesOrNo canAllocateBailiff;

    /**
     * Gates "Set risk level": the current enforcement is an approved, live warrant.
     */
    @CCD(searchable = false, access = {InternalTabAccess.class})
    private YesOrNo canSetRiskLevel;

    /**
     * Gates "Add a bailiff note": the claim has an enforcement.
     */
    @CCD(searchable = false, access = {InternalTabAccess.class})
    private YesOrNo canAddBailiffNote;
}
