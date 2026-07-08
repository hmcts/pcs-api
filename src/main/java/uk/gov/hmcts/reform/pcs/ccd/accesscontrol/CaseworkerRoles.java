package uk.gov.hmcts.reform.pcs.ccd.accesscontrol;

public final class CaseworkerRoles {

    public static final UserRole[] CASEWORKER_ROLES = {
        UserRole.HEARING_CENTRE_TEAM_LEADER,
        UserRole.HEARING_CENTRE_ADMIN,
        UserRole.CTSC_TEAM_LEADER,
        UserRole.CTSC_ADMIN,
        UserRole.WLU_TEAM_LEADER,
        UserRole.WLU_ADMIN
    };

    private CaseworkerRoles() {
    }
}
