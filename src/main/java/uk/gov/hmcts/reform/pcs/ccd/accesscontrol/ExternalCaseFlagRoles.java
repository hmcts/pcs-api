package uk.gov.hmcts.reform.pcs.ccd.accesscontrol;

public final class ExternalCaseFlagRoles {

    public static final UserRole[] EXTERNAL_CASE_FLAG_ROLES = {
        UserRole.PCS_SOLICITOR,
        UserRole.CITIZEN,
        UserRole.CLAIMANT_SOLICITOR,
        UserRole.DEFENDANT,
        UserRole.DEFENDANT_SOLICITOR,
        UserRole.CLAIMANT,
        UserRole.GA_CLAIMANT_SOLICITOR,
        UserRole.GA_DEFENDANT_SOLICITOR
    };

    public static final UserRole[] DEFENDANT_SUPPORT_REQUEST_ROLES = {
        UserRole.CITIZEN,
        UserRole.DEFENDANT,
        UserRole.DEFENDANT_SOLICITOR,
        UserRole.GA_DEFENDANT_SOLICITOR
    };

    private ExternalCaseFlagRoles() {
    }
}
