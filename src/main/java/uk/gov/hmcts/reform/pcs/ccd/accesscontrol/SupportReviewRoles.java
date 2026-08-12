package uk.gov.hmcts.reform.pcs.ccd.accesscontrol;

public final class SupportReviewRoles {

    public static final UserRole[] SUPPORT_REVIEW_ROLES = {
        UserRole.CTSC_TEAM_LEADER,
        UserRole.CTSC_ADMIN,
        UserRole.HEARING_CENTRE_TEAM_LEADER,
        UserRole.HEARING_CENTRE_ADMIN,
        UserRole.WLU_TEAM_LEADER,
        UserRole.WLU_ADMIN
    };

    private SupportReviewRoles() {
    }
}
