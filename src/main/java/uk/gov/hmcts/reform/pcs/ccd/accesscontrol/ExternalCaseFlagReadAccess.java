package uk.gov.hmcts.reform.pcs.ccd.accesscontrol;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import uk.gov.hmcts.ccd.sdk.api.HasAccessControl;
import uk.gov.hmcts.ccd.sdk.api.HasRole;
import uk.gov.hmcts.ccd.sdk.api.Permission;

import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.ExternalCaseFlagRoles.EXTERNAL_CASE_FLAG_ROLES;

public class ExternalCaseFlagReadAccess implements HasAccessControl {

    @Override
    public SetMultimap<HasRole, Permission> getGrants() {
        SetMultimap<HasRole, Permission> grants = HashMultimap.create();
        for (UserRole externalRole : EXTERNAL_CASE_FLAG_ROLES) {
            grants.put(externalRole, Permission.R);
        }

        return grants;
    }
}
