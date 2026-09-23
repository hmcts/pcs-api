package uk.gov.hmcts.reform.pcs.ccd.accesscontrol;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import uk.gov.hmcts.ccd.sdk.api.HasAccessControl;
import uk.gov.hmcts.ccd.sdk.api.HasRole;
import uk.gov.hmcts.ccd.sdk.api.Permission;

import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.CIRCUIT_JUDGE;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.FEE_PAID_JUDGE;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.JUDGE;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.LEADERSHIP_JUDGE;

public class MakeOrderAccess implements HasAccessControl {

    @Override
    public SetMultimap<HasRole, Permission> getGrants() {
        SetMultimap<HasRole, Permission> grants = HashMultimap.create();
        grants.putAll(JUDGE, Permission.CRU);
        grants.putAll(FEE_PAID_JUDGE, Permission.CRU);
        grants.putAll(CIRCUIT_JUDGE, Permission.CRU);
        grants.putAll(LEADERSHIP_JUDGE, Permission.CRU);
        return grants;
    }
}
