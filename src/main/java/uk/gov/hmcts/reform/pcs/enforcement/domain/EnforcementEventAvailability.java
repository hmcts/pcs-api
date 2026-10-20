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
 * Flags for each enforcement event's CCD show condition, so an event its start callback would refuse is not
 * offered in the next step drop down. The start callbacks stay as the backstop.
 */
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EnforcementEventAvailability {

    @CCD(searchable = false, access = {ClaimantEventFlagAccess.class})
    private YesOrNo canApplyForEnforcement;

    @CCD(searchable = false, access = {InternalTabAccess.class})
    private YesOrNo canDecideEnforcement;

    @CCD(searchable = false, access = {InternalTabAccess.class})
    private YesOrNo canAllocateBailiff;

    @CCD(searchable = false, access = {InternalTabAccess.class})
    private YesOrNo canSetRiskLevel;

    @CCD(searchable = false, access = {InternalTabAccess.class})
    private YesOrNo canAddBailiffNote;
}
