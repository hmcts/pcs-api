package uk.gov.hmcts.reform.pcs.enforcement.accesscontrol;

/**
 * Which part of the enforcement a user viewing a case may see, decided by {@link EnforcementViewerService}.
 */
public enum EnforcementViewer {

    /**
     * Judiciary, caseworkers, bailiff staff and PCS's system users: everything.
     */
    INTERNAL,

    /**
     * The claimant and their legal representatives on this case: everything but the internal-only documents
     * and sections.
     */
    CLAIMANT,

    /**
     * Defendants and their legal representatives on this case, and anyone whose side of the case cannot be
     * established: only the warrants whose notice of eviction has been served.
     */
    RESTRICTED;

    public boolean isRestricted() {
        return this == RESTRICTED;
    }
}
