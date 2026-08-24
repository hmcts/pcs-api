package uk.gov.hmcts.reform.pcs.ccd.accesscontrol;

/**
 * Organisation profile ids as PRD holds them. The id is the constant name - keeping them identical
 * means a rename cannot leave the two out of step, which would emit a profile id matching nothing
 * in PRD and fail silently rather than at compile time.
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
