package uk.gov.hmcts.reform.pcs.ccd.accesscontrol;

import com.google.common.collect.SetMultimap;
import uk.gov.hmcts.ccd.sdk.api.HasAccessControl;
import uk.gov.hmcts.ccd.sdk.api.HasRole;
import uk.gov.hmcts.ccd.sdk.api.Permission;

/** Read-only risk assessment access for the internal staff and judiciary personas. */
public class RiskAssessmentInternalAccess implements HasAccessControl {

    @Override
    public SetMultimap<HasRole, Permission> getGrants() {
        SetMultimap<HasRole, Permission> grants = AccessGrants.internalUserReadAccess();
        grants.put(UserRole.BAILIFF, Permission.R);
        grants.put(UserRole.BAILIFF_MANAGER, Permission.R);
        grants.put(UserRole.HEARING_CENTRE_TEAM_LEADER, Permission.R);
        grants.put(UserRole.CTSC_TEAM_LEADER, Permission.R);
        grants.put(UserRole.WLU_TEAM_LEADER, Permission.R);
        return grants;
    }
}
