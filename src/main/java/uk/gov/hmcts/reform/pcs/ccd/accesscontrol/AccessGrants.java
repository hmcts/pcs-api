package uk.gov.hmcts.reform.pcs.ccd.accesscontrol;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import uk.gov.hmcts.ccd.sdk.api.HasRole;
import uk.gov.hmcts.ccd.sdk.api.Permission;

import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.CIRCUIT_JUDGE;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.CITIZEN;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.CLAIMANT_SOLICITOR;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.CREATOR;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.CTSC_ADMIN;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.CTSC_TEAM_LEADER;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.DEFENDANT;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.DEFENDANT_SOLICITOR;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.FEE_PAID_JUDGE;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.HEARING_CENTRE_ADMIN;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.HEARING_CENTRE_TEAM_LEADER;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.JUDGE;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.LEADERSHIP_JUDGE;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.PCS_CASE_WORKER;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.PCS_SOLICITOR;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.WLU_ADMIN;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.WLU_TEAM_LEADER;

final class AccessGrants {

    static final UserRole[] PARTY_VISIBLE_ROLES = {
        CREATOR,
        CITIZEN,
        DEFENDANT,
        CLAIMANT_SOLICITOR,
        DEFENDANT_SOLICITOR,
        JUDGE,
        FEE_PAID_JUDGE,
        CIRCUIT_JUDGE,
        LEADERSHIP_JUDGE,
        HEARING_CENTRE_TEAM_LEADER,
        HEARING_CENTRE_ADMIN,
        CTSC_TEAM_LEADER,
        CTSC_ADMIN,
        WLU_TEAM_LEADER,
        WLU_ADMIN
    };

    static final UserRole[] INTERNAL_READ_ROLES = {
        JUDGE,
        FEE_PAID_JUDGE,
        CIRCUIT_JUDGE,
        LEADERSHIP_JUDGE,
        HEARING_CENTRE_TEAM_LEADER,
        HEARING_CENTRE_ADMIN,
        CTSC_TEAM_LEADER,
        CTSC_ADMIN,
        WLU_TEAM_LEADER,
        WLU_ADMIN
    };

    private AccessGrants() {
    }

    static SetMultimap<HasRole, Permission> partyVisibleReadAccess() {
        return readAccess(PARTY_VISIBLE_ROLES);
    }

    static SetMultimap<HasRole, Permission> documentAccess() {
        SetMultimap<HasRole, Permission> grants = HashMultimap.create();
        grants.putAll(PCS_SOLICITOR, Permission.CR);
        grants.putAll(CITIZEN, Permission.CR);
        grants.putAll(DEFENDANT, Permission.CR);
        grants.putAll(CLAIMANT_SOLICITOR, Permission.CR);
        grants.putAll(DEFENDANT_SOLICITOR, Permission.CR);
        addReadAccess(grants, INTERNAL_READ_ROLES);
        return grants;
    }

    static SetMultimap<HasRole, Permission> caseLinkingAccess() {
        SetMultimap<HasRole, Permission> grants = HashMultimap.create();
        grants.putAll(PCS_SOLICITOR, Permission.CRU);
        grants.put(PCS_CASE_WORKER, Permission.R);
        addReadAccess(grants, INTERNAL_READ_ROLES);
        return grants;
    }

    private static SetMultimap<HasRole, Permission> readAccess(UserRole... roles) {
        SetMultimap<HasRole, Permission> grants = HashMultimap.create();
        addReadAccess(grants, roles);
        return grants;
    }

    private static void addReadAccess(SetMultimap<HasRole, Permission> grants, UserRole... roles) {
        for (UserRole role : roles) {
            grants.put(role, Permission.R);
        }
    }
}
