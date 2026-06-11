package uk.gov.hmcts.reform.pcs.ccd.accesscontrol;

import com.google.common.collect.SetMultimap;
import uk.gov.hmcts.ccd.sdk.api.HasAccessControl;
import uk.gov.hmcts.ccd.sdk.api.HasRole;
import uk.gov.hmcts.ccd.sdk.api.Permission;

public class CaseLinkingAccess implements HasAccessControl {


    @Override
    public SetMultimap<HasRole, Permission> getGrants() {
        return AccessGrants.caseLinkingAccess();
    }
}
