package uk.gov.hmcts.reform.pcs.ccd.accesscontrol;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import uk.gov.hmcts.ccd.sdk.api.HasAccessControl;
import uk.gov.hmcts.ccd.sdk.api.HasRole;
import uk.gov.hmcts.ccd.sdk.api.Permission;

import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.PCS_CASE_WORKER;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.PCS_SOLICITOR;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.CIRCUIT_JUDGE;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.CTSC_ADMIN;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.CTSC_TEAM_LEADER;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.FEE_PAID_JUDGE;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.HEARING_CENTRE_ADMIN;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.HEARING_CENTRE_TEAM_LEADER;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.JUDGE;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.LEADERSHIP_JUDGE;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.WLU_ADMIN;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.WLU_TEAM_LEADER;

public class CaseLinkingAccess implements HasAccessControl {


    @Override
    public SetMultimap<HasRole, Permission> getGrants() {
        SetMultimap<HasRole, Permission> grants = HashMultimap.create();
        grants.putAll(PCS_SOLICITOR, Permission.CRU);
        grants.put(PCS_CASE_WORKER, Permission.R);
        grants.put(JUDGE, Permission.R);
        grants.put(FEE_PAID_JUDGE, Permission.R);
        grants.put(CIRCUIT_JUDGE, Permission.R);
        grants.put(LEADERSHIP_JUDGE, Permission.R);
        grants.put(HEARING_CENTRE_TEAM_LEADER, Permission.R);
        grants.put(HEARING_CENTRE_ADMIN, Permission.R);
        grants.put(CTSC_TEAM_LEADER, Permission.R);
        grants.put(CTSC_ADMIN, Permission.R);
        grants.put(WLU_TEAM_LEADER, Permission.R);
        grants.put(WLU_ADMIN, Permission.R);

        return grants;
    }
}
