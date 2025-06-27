package uk.gov.hmcts.reform.pcs.ccd.accesscontrol;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import uk.gov.hmcts.ccd.sdk.api.HasAccessControl;
import uk.gov.hmcts.ccd.sdk.api.HasRole;
import uk.gov.hmcts.ccd.sdk.api.Permission;

import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.HOUSING_PROVIDER;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.PCS_CASE_WORKER;


public class CaseworkerAccess implements HasAccessControl {

    @Override
    public SetMultimap<HasRole, Permission> getGrants() {
        SetMultimap<HasRole, Permission> grants = HashMultimap.create();
        grants.putAll(PCS_CASE_WORKER, Permission.CRU);
        grants.putAll(HOUSING_PROVIDER, Permission.CRU);
        return grants;
    }

}
