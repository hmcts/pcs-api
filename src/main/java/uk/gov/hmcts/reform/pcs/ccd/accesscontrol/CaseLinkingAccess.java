package uk.gov.hmcts.reform.pcs.ccd.accesscontrol;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import uk.gov.hmcts.ccd.sdk.api.HasAccessControl;
import uk.gov.hmcts.ccd.sdk.api.HasRole;
import uk.gov.hmcts.ccd.sdk.api.Permission;

import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.AccessProfile.CIRCUIT_JUDGE_WRITE;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.AccessProfile.FEE_PAID_JUDGE_WRITE;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.AccessProfile.JUDGE_WRITE;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.AccessProfile.LEADERSHIP_JUDGE_WRITE;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.CTSC_ADMIN;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.HEARING_CENTRE_ADMIN;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.PCS_SOLICITOR;

public class CaseLinkingAccess implements HasAccessControl {


    @Override
    public SetMultimap<HasRole, Permission> getGrants() {
        SetMultimap<HasRole, Permission> grants = HashMultimap.create();
        grants.putAll(PCS_SOLICITOR, Permission.CRU);
        grants.putAll(HEARING_CENTRE_ADMIN, Permission.CRU);
        grants.putAll(CTSC_ADMIN, Permission.CRU);
        grants.putAll(CIRCUIT_JUDGE_WRITE, Permission.CRU);
        grants.putAll(FEE_PAID_JUDGE_WRITE, Permission.CRU);
        grants.putAll(JUDGE_WRITE, Permission.CRU);
        grants.putAll(LEADERSHIP_JUDGE_WRITE, Permission.CRU);
        grants.put(AccessProfile.WLU_ADMIN_READ, Permission.R);

        return grants;
    }
}
