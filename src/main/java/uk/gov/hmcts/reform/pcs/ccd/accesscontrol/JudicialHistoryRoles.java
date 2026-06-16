package uk.gov.hmcts.reform.pcs.ccd.accesscontrol;

public final class JudicialHistoryRoles {

    public static final UserRole[] JUDICIAL_HISTORY_ROLES = {
        UserRole.CIRCUIT_JUDGE,
        UserRole.FEE_PAID_JUDGE,
        UserRole.JUDGE,
        UserRole.LEADERSHIP_JUDGE
    };

    private JudicialHistoryRoles() {
    }
}
