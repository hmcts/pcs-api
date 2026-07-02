package uk.gov.hmcts.reform.pcs.ccd.accesscontrol;

import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.AccessProfile.CTSC_ADMIN;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.AccessProfile.HEARING_CENTRE_ADMIN;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.AccessProfile.JUDGE;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.AccessProfile.WLU_ADMIN;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import uk.gov.hmcts.ccd.sdk.api.HasAccessControl;
import uk.gov.hmcts.ccd.sdk.api.HasRole;
import uk.gov.hmcts.ccd.sdk.api.Permission;

public class CaseLinkingAccess implements HasAccessControl {


    @Override
    public SetMultimap<HasRole, Permission> getGrants() {
        SetMultimap<HasRole, Permission> grants = HashMultimap.create();
        grants.putAll(JUDGE, Permission.CRU);
        grants.putAll(CTSC_ADMIN, Permission.CRU);
        grants.putAll(HEARING_CENTRE_ADMIN, Permission.CRU);
        grants.put(WLU_ADMIN, Permission.R);
        return grants;
    }
}
