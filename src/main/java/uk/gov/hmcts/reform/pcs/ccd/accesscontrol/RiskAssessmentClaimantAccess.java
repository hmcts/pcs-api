package uk.gov.hmcts.reform.pcs.ccd.accesscontrol;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import uk.gov.hmcts.ccd.sdk.api.HasAccessControl;
import uk.gov.hmcts.ccd.sdk.api.HasRole;
import uk.gov.hmcts.ccd.sdk.api.Permission;

/**
 * Read-only claimant view for claimants and their representatives, including litigation friends
 * acting with claimant access. Use claimant-side assignments rather than generic IDAM roles.
 */
public class RiskAssessmentClaimantAccess implements HasAccessControl {

    @Override
    public SetMultimap<HasRole, Permission> getGrants() {
        SetMultimap<HasRole, Permission> grants = HashMultimap.create();
        grants.put(UserRole.CLAIMANT, Permission.R);
        grants.put(UserRole.GA_CLAIMANT_SOLICITOR, Permission.R);
        grants.put(UserRole.CLAIMANT_SOLICITOR, Permission.R);
        return grants;
    }
}
