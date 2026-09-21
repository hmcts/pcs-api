package uk.gov.hmcts.reform.pcs.ccd.accesscontrol;

/**
 * Organisation profile IDs as they appear in PRD.
 * Must be kept in sync.
 */
public enum OrganisationProfile {

    SOLICITOR_PROFILE,
    LOCALAUTH_PROFILE,
    OTHER_REALT_PROFILE,
    OTHER_PROP_PROFILE,
    OTHER_NFP_PROFILE,
    OTHER_CHARITY_PROFILE;

    public String getId() {
        return name();
    }
}
