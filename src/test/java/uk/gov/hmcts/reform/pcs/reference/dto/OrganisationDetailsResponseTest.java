package uk.gov.hmcts.reform.pcs.reference.dto;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrganisationDetailsResponseTest {

    @Test
    void shouldPreferSpecificProfileOverGenericOrganisationProfile() {
        OrganisationDetailsResponse response = OrganisationDetailsResponse.builder()
            .organisationIdentifier("ORG1")
            .organisationProfileIds(List.of("ORGANISATION_PROFILE", "LOCALAUTH_PROFILE"))
            .build();

        assertThat(response.getOrgProfileId()).isEqualTo("LOCALAUTH_PROFILE");
    }

    @Test
    void shouldFallBackToGenericOrganisationProfileWhenItIsTheOnlyProfile() {
        OrganisationDetailsResponse response = OrganisationDetailsResponse.builder()
            .organisationIdentifier("ORG1")
            .organisationProfileIds(List.of("ORGANISATION_PROFILE"))
            .build();

        assertThat(response.getOrgProfileId()).isEqualTo("ORGANISATION_PROFILE");
    }

    @Test
    void shouldRejectMissingProfileIds() {
        OrganisationDetailsResponse response = OrganisationDetailsResponse.builder()
            .organisationIdentifier("ORG1")
            .build();

        assertThatThrownBy(response::getOrgProfileId)
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("No organisation profile id found");
    }

    @Test
    void shouldRejectMultipleSpecificProfiles() {
        OrganisationDetailsResponse response = OrganisationDetailsResponse.builder()
            .organisationIdentifier("ORG1")
            .organisationProfileIds(List.of("LOCALAUTH_PROFILE", "OTHER_REALT_PROFILE"))
            .build();

        assertThatThrownBy(response::getOrgProfileId)
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("more than one profile");
    }
}
