package uk.gov.hmcts.reform.pcs.ccd.accesscontrol;

/**
 * The PRD organisation profile IDs that PCS maps to a group access type. Any other claimant
 * profile falls back to the generic {@link #ORGANISATION_PROFILE} access, so this does not need
 * to list every profile PRD knows about.
 */
public enum OrganisationProfile {

    ORGANISATION_PROFILE,
    SOLICITOR_PROFILE;

    public String getId() {
        return name();
    }
}
