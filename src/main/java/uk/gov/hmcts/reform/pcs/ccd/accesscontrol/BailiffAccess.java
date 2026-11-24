package uk.gov.hmcts.reform.pcs.ccd.accesscontrol;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import uk.gov.hmcts.ccd.sdk.api.HasAccessControl;
import uk.gov.hmcts.ccd.sdk.api.HasRole;
import uk.gov.hmcts.ccd.sdk.api.Permission;

import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.BAILIFF;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.BAILIFF_ADMIN;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.BAILIFF_MANAGER;

/** Reusable field policy; events must explicitly grant their own bailiff permissions. */
public class BailiffAccess implements HasAccessControl {

    @Override
    public SetMultimap<HasRole, Permission> getGrants() {
        SetMultimap<HasRole, Permission> grants = HashMultimap.create();
        grants.putAll(BAILIFF, Permission.CRU);
        grants.putAll(BAILIFF_ADMIN, Permission.CRU);
        grants.putAll(BAILIFF_MANAGER, Permission.CRU);
        return grants;
    }
}
