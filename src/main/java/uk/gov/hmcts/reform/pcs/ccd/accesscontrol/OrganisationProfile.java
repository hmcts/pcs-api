package uk.gov.hmcts.reform.pcs.ccd.accesscontrol;

import lombok.Getter;

/**
 * The Reference Data organisation profiles PCS grants access types against.
 *
 * <p>IDs must match the profiles rd-professional-api assigns to organisations
 * (see {@code OrganisationProfileIdConstants} there): PRM only mints group role assignments for an
 * access type when the organisation carries the matching profile.</p>
 */
@Getter
public enum OrganisationProfile {

    LOCALAUTH_PROFILE("LOCALAUTH_PROFILE"),
    SOLICITOR_PROFILE("SOLICITOR_PROFILE"),
    OTHER_REALT_PROFILE("OTHER_REALT_PROFILE"),
    OTHER_PROP_PROFILE("OTHER_PROP_PROFILE"),
    OTHER_NFP_PROFILE("OTHER_NFP_PROFILE"),
    OTHER_CHARITY_PROFILE("OTHER_CHARITY_PROFILE");

    private final String id;

    OrganisationProfile(String id) {
        this.id = id;
    }
}
