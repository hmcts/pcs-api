package uk.gov.hmcts.reform.pcs.ccd.accesscontrol;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OrganisationProfileTest {

    @Test
    void shouldExposePrdProfileIdAsEnumName() {
        assertThat(OrganisationProfile.ORGANISATION_PROFILE.getId()).isEqualTo("ORGANISATION_PROFILE");
        assertThat(OrganisationProfile.SOLICITOR_PROFILE.getId()).isEqualTo("SOLICITOR_PROFILE");
        assertThat(OrganisationProfile.LOCALAUTH_PROFILE.getId()).isEqualTo("LOCALAUTH_PROFILE");
    }
}
