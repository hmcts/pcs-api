package uk.gov.hmcts.reform.pcs.ccd.accesscontrol;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GroupAccessTypeTest {

    /**
     * The lookup is keyed on organisation profile and party role, so two access types sharing a key
     * would leave one unreachable and stamp cases with the other's group id - which reads as "the
     * user cannot see the case" rather than as a configuration mistake.
     */
    @Test
    void shouldDeclareOneAccessTypePerOrganisationProfileAndPartyRole() {
        List<String> keys = Arrays.stream(GroupAccessType.values())
            .filter(accessType -> accessType.getPartyRole() != null)
            .map(accessType -> accessType.getOrganisationProfileId() + " + " + accessType.getPartyRole())
            .toList();

        assertThat(keys).doesNotHaveDuplicates();
    }

    @Test
    void shouldResolveTheGroupIdTemplateForEachDeclaredCombination() {
        Arrays.stream(GroupAccessType.values())
            .filter(accessType -> accessType.getPartyRole() != null)
            .forEach(accessType -> assertThat(GroupAccessType.caseAccessGroupIdFor(
                accessType.getOrganisationProfileId(), accessType.getPartyRole(), "$ORGID$"))
                .contains(accessType.getCaseAccessGroupIdTemplate()));
    }

    @Test
    void shouldReturnEmptyWhenOrganisationProfileIdIsNull() {
        assertThat(GroupAccessType.caseAccessGroupIdFor(null, null, "ORG123")).isEmpty();
    }

    @Test
    void shouldReturnEmptyWhenPartyRoleIsNull() {
        assertThat(GroupAccessType.caseAccessGroupIdFor("SOLICITOR_PROFILE", null, "ORG123")).isEmpty();
    }

    @Test
    void shouldReturnEmptyForUnmappedDefendantProfile() {
        assertThat(GroupAccessType.caseAccessGroupIdFor("LOCALAUTH_PROFILE", PartyRole.DEFENDANT, "ORG123"))
            .isEmpty();
    }

    @Test
    void shouldBuildCaseAccessGroupIdTemplateInJurisdictionCaseTypeAccessTypeRoleOrgidFormat() {
        assertThat(GroupAccessType.SOLICITOR_ORG_CLAIMANT_ACCESS.getCaseAccessGroupIdTemplate())
            .isEqualTo("PCS:PCS:solicitor-org-claimant-access:claimant-solicitor:$ORGID$");
        assertThat(GroupAccessType.PROFESSIONAL_ORG_CLAIMANT_ACCESS.getCaseAccessGroupIdTemplate())
            .isEqualTo("PCS:PCS:prof-org-claimant-access:claimant:$ORGID$");
    }

    @Test
    void shouldExposeFarFutureLiveToDateRequiredByDefinitionImport() {
        assertThat(GroupAccessType.PROFESSIONAL_ORG_CLAIMANT_ACCESS.getLiveTo()).isEqualTo("01/01/2099");
    }

    @Test
    void shouldResolveClaimantAccessForGenericOrganisationProfile() {
        assertThat(GroupAccessType.caseAccessGroupIdFor(
            OrganisationProfile.ORGANISATION_PROFILE.getId(), PartyRole.CLAIMANT, "ORG123"))
            .contains("PCS:PCS:prof-org-claimant-access:claimant:ORG123");
    }

    /**
     * PRD profiles PCS has no access type of its own for: their claimants still get the generic
     * organisation claimant access.
     */
    @ParameterizedTest
    @ValueSource(strings = {
        "LOCALAUTH_PROFILE", "OTHER_REALT_PROFILE", "OTHER_PROP_PROFILE", "OTHER_NFP_PROFILE", "OTHER_CHARITY_PROFILE"
    })
    void shouldResolveClaimantAccessForStampedSpecificOrganisationProfiles(String orgProfileId) {
        assertThat(GroupAccessType.caseAccessGroupIdFor(orgProfileId, PartyRole.CLAIMANT, "ORG123"))
            .contains("PCS:PCS:prof-org-claimant-access:claimant:ORG123");
    }

    @Test
    void shouldStillPreferSolicitorClaimantAccessOverProfessionalOrgFallback() {
        assertThat(GroupAccessType.caseAccessGroupIdFor(
            OrganisationProfile.SOLICITOR_PROFILE.getId(), PartyRole.CLAIMANT, "ORG123"))
            .contains("PCS:PCS:solicitor-org-claimant-access:claimant-solicitor:ORG123");
    }

    @Test
    void shouldExposeOrganisationProfileIdsFromEnumConstants() {
        assertThat(GroupAccessType.PROFESSIONAL_ORG_CLAIMANT_ACCESS.getOrganisationProfileId())
            .isEqualTo(OrganisationProfile.ORGANISATION_PROFILE.getId());
        assertThat(GroupAccessType.SOLICITOR_ORG_CLAIMANT_ACCESS.getOrganisationProfileId())
            .isEqualTo(OrganisationProfile.SOLICITOR_PROFILE.getId());
    }
}
