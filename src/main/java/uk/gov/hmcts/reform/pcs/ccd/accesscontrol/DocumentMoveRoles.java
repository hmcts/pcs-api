package uk.gov.hmcts.reform.pcs.ccd.accesscontrol;

public final class DocumentMoveRoles {

    public static final UserRole[] DOCUMENT_MOVE_ROLES = {
        UserRole.JUDGE,
        UserRole.FEE_PAID_JUDGE,
        UserRole.CIRCUIT_JUDGE,
        UserRole.LEADERSHIP_JUDGE,
        UserRole.HEARING_CENTRE_ADMIN,
        UserRole.HEARING_CENTRE_TEAM_LEADER,
        UserRole.CTSC_ADMIN,
        UserRole.CTSC_TEAM_LEADER,
        UserRole.WLU_ADMIN,
        UserRole.WLU_TEAM_LEADER
    };

    private DocumentMoveRoles() {
    }

}
