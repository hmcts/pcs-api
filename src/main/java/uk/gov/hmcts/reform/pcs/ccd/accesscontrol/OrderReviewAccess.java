package uk.gov.hmcts.reform.pcs.ccd.accesscontrol;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import uk.gov.hmcts.ccd.sdk.api.HasAccessControl;
import uk.gov.hmcts.ccd.sdk.api.HasRole;
import uk.gov.hmcts.ccd.sdk.api.Permission;

import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.CaseworkerRoles.CASEWORKER_ROLES;

/**
 * Read access for the caseworkers the confirm order review event is granted to. The court staff
 * rendering of the draft orders tab, which the field is on, lets the rest of the court staff read it too.
 */
public class OrderReviewAccess implements HasAccessControl {

    @Override
    public SetMultimap<HasRole, Permission> getGrants() {
        SetMultimap<HasRole, Permission> grants = HashMultimap.create();
        for (UserRole role : CASEWORKER_ROLES) {
            grants.put(role, Permission.R);
        }
        return grants;
    }
}
