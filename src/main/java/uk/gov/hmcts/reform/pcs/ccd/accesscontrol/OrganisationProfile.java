package uk.gov.hmcts.reform.pcs.ccd.accesscontrol;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum OrganisationProfile {

    SOLICITOR_PROFILE("SOLICITOR_PROFILE"),
    LOCALAUTH_PROFILE("LOCALAUTH_PROFILE"),
    OTHER_REALT_PROFILE("OTHER_REALT_PROFILE"),
    OTHER_PROP_PROFILE("OTHER_PROP_PROFILE"),
    OTHER_NFP_PROFILE("OTHER_NFP_PROFILE"),
    OTHER_CHARITY_PROFILE("OTHER_CHARITY_PROFILE");

    private final String id;
}
