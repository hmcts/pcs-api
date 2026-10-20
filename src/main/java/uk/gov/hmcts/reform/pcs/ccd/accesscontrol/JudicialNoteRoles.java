package uk.gov.hmcts.reform.pcs.ccd.accesscontrol;

public class JudicialNoteRoles {

    public static final UserRole[] JUDICIAL_NOTES_ROLES = {
        UserRole.JUDGE,
        UserRole.CIRCUIT_JUDGE,
        UserRole.FEE_PAID_JUDGE,
        UserRole.LEADERSHIP_JUDGE
    };

    private JudicialNoteRoles() {
    }
}
