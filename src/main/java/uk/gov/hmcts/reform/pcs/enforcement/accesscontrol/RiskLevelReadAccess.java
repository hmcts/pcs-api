package uk.gov.hmcts.reform.pcs.enforcement.accesscontrol;

import com.google.common.collect.SetMultimap;
import uk.gov.hmcts.ccd.sdk.api.HasAccessControl;
import uk.gov.hmcts.ccd.sdk.api.HasRole;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.InternalTabAccess;

/**
 * Read only access to the saved eviction risk level and its reason for internal users, bailiffs included.
 * Claimants and defendants get none. The level is set through the "Set risk level" event, which grants its
 * own access to the fields it captures.
 */
public class RiskLevelReadAccess implements HasAccessControl {

    @Override
    public SetMultimap<HasRole, Permission> getGrants() {
        return new InternalTabAccess().getGrants();
    }
}
