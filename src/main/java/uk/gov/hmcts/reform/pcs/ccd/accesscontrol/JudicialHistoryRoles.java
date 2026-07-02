package uk.gov.hmcts.reform.pcs.ccd.accesscontrol;

public final class JudicialHistoryRoles {

    public static final UserRole[] JUDICIAL_HISTORY_ROLES = {
        UserRole.CTSC_ADMIN,
        UserRole.CTSC_TEAM_LEADER,
        UserRole.CIRCUIT_JUDGE,
        UserRole.FEE_PAID_JUDGE,
        UserRole.JUDGE,
        UserRole.LEADERSHIP_JUDGE,
        UserRole.WLU_ADMIN,
        UserRole.WLU_TEAM_LEADER
    };

    private JudicialHistoryRoles() {
    }
}
