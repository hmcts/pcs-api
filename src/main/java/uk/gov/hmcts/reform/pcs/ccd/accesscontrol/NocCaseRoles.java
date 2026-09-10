package uk.gov.hmcts.reform.pcs.ccd.accesscontrol;

/**
 * Case role names Notice of Change is keyed on, held as plain constants so that {@link UserRole},
 * {@link AccessProfile} and {@link GroupAccessType} can all name the same value without referencing
 * each other during enum initialisation.
 */
public final class NocCaseRoles {

    /**
     * The role every defendant's OrganisationPolicy carries and the NoC challenge question answers
     * with. It grants nothing: defendant solicitors reach cases through the {@code defendant-solicitor}
     * organisation role. It exists because CCD's ChallengeQuestion import and ACA's noc-questions
     * endpoint both require a bracketed case role here.
     */
    public static final String DEFENDANT = "[DEFENDANTSOLICITOR]";

    private NocCaseRoles() {
    }
}
