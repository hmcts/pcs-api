package uk.gov.hmcts.reform.pcs.ccd.accesscontrol;

public final class CaseNoteRoles {

    public static final UserRole[] CASE_NOTE_ROLES = {
        UserRole.HEARING_CENTRE_TEAM_LEADER,
        UserRole.HEARING_CENTRE_ADMIN,
        UserRole.CTSC_TEAM_LEADER,
        UserRole.CTSC_ADMIN,
        UserRole.CIRCUIT_JUDGE,
        UserRole.FEE_PAID_JUDGE,
        UserRole.JUDGE,
        UserRole.LEADERSHIP_JUDGE,
        UserRole.WLU_TEAM_LEADER,
        UserRole.WLU_ADMIN
    };

    private CaseNoteRoles() {
    }
}
