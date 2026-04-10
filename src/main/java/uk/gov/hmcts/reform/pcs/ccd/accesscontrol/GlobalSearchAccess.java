package uk.gov.hmcts.reform.pcs.ccd.accesscontrol;

import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.GS_PROFILE;

import uk.gov.hmcts.ccd.sdk.api.HasAccessControl;
import uk.gov.hmcts.ccd.sdk.api.HasRole;
import uk.gov.hmcts.ccd.sdk.api.Permission;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;

public class GlobalSearchAccess implements HasAccessControl {
    @Override
    public SetMultimap<HasRole, Permission> getGrants() {
        SetMultimap<HasRole, Permission> grants = HashMultimap.create();
        grants.put(GS_PROFILE, Permission.R);

        return grants;
    }
}
