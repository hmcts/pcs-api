package uk.gov.hmcts.reform.pcs.ccd.accesscontrol;

public final class ExternalCaseFlagRoles {

    /**
     * External users who may see and change support on a case. The group access profiles are included
     * alongside the case roles because a professional whose access comes through their organisation's
     * group access holds claimant-solicitor / defendant-solicitor / claimant instead of a case role, and
     * without a field authorisation the Support tab has nothing to render for them. Which party's support
     * each of these users can see and change is enforced per party by PartySupportOwnershipResolver, so
     * this list does not widen cross-party access.
     */
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

    private ExternalCaseFlagRoles() {
    }
}
