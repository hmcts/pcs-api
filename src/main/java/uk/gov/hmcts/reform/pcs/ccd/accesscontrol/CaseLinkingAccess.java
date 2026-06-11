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
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.CTSC_ADMIN;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.HEARING_CENTRE_ADMIN;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.AccessProfile.CASE_LINK_READ;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.AccessProfile.PCS_SOLICITOR;

public class CaseLinkingAccess implements HasAccessControl {


    @Override
    public SetMultimap<HasRole, Permission> getGrants() {
        SetMultimap<HasRole, Permission> grants = HashMultimap.create();
        grants.putAll(PCS_SOLICITOR, Permission.CRU);
        grants.putAll(CIRCUIT_JUDGE, Permission.CRU);
        grants.putAll(FEE_PAID_JUDGE, Permission.CRU);
        grants.putAll(JUDGE, Permission.CRU);
        grants.putAll(LEADERSHIP_JUDGE, Permission.CRU);
        grants.putAll(CTSC_ADMIN, Permission.CRU);
        grants.putAll(HEARING_CENTRE_ADMIN, Permission.CRU);
        grants.put(CASE_LINK_READ, Permission.R);

        return grants;
    }
}
