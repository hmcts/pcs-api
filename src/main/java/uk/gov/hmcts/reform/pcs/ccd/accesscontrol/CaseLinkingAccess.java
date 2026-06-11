package uk.gov.hmcts.reform.pcs.ccd.accesscontrol;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import uk.gov.hmcts.ccd.sdk.api.HasAccessControl;
import uk.gov.hmcts.ccd.sdk.api.HasRole;
import uk.gov.hmcts.ccd.sdk.api.Permission;

import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.AccessProfile.CASE_LINK_READ;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.AccessProfile.CASE_LINK_WRITE;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.AccessProfile.PCS_SOLICITOR;

public class CaseLinkingAccess implements HasAccessControl {


    @Override
    public SetMultimap<HasRole, Permission> getGrants() {
        SetMultimap<HasRole, Permission> grants = HashMultimap.create();
        grants.putAll(PCS_SOLICITOR, Permission.CRU);
        grants.putAll(CASE_LINK_WRITE, Permission.CRU);
        grants.put(CASE_LINK_READ, Permission.R);

        return grants;
    }
}
