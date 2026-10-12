package uk.gov.hmcts.reform.pcs.ccd.accesscontrol;

import org.junit.jupiter.api.Test;
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
    void shouldBuildCaseAccessGroupIdTemplateInJurisdictionCaseTypeAccessTypeRoleOrgidFormat() {
        assertThat(GroupAccessType.SOLICITOR_ORG_CLAIMANT_ACCESS.getCaseAccessGroupIdTemplate())
            .isEqualTo("PCS:PCS:solicitor-org-claimant-access:claimant-solicitor:$ORGID$");
    }

    @Test
    void shouldResolveClaimantAccessForGenericOrganisationProfile() {
        assertThat(GroupAccessType.caseAccessGroupIdFor("ORGANISATION_PROFILE", PartyRole.CLAIMANT, "ORG123"))
            .contains("PCS:PCS:prof-org-claimant-access:claimant:ORG123");
    }

    @Test
    void shouldResolveClaimantAccessForStampedSpecificOrganisationProfiles() {
        assertThat(GroupAccessType.caseAccessGroupIdFor("LOCALAUTH_PROFILE", PartyRole.CLAIMANT, "ORG123"))
            .contains("PCS:PCS:prof-org-claimant-access:claimant:ORG123");
        assertThat(GroupAccessType.caseAccessGroupIdFor("OTHER_REALT_PROFILE", PartyRole.CLAIMANT, "ORG123"))
            .contains("PCS:PCS:prof-org-claimant-access:claimant:ORG123");
        assertThat(GroupAccessType.caseAccessGroupIdFor("OTHER_PROP_PROFILE", PartyRole.CLAIMANT, "ORG123"))
            .contains("PCS:PCS:prof-org-claimant-access:claimant:ORG123");
        assertThat(GroupAccessType.caseAccessGroupIdFor("OTHER_NFP_PROFILE", PartyRole.CLAIMANT, "ORG123"))
            .contains("PCS:PCS:prof-org-claimant-access:claimant:ORG123");
        assertThat(GroupAccessType.caseAccessGroupIdFor("OTHER_CHARITY_PROFILE", PartyRole.CLAIMANT, "ORG123"))
            .contains("PCS:PCS:prof-org-claimant-access:claimant:ORG123");
    }

    @Test
    void shouldStillPreferSolicitorClaimantAccessOverProfessionalOrgFallback() {
        assertThat(GroupAccessType.caseAccessGroupIdFor("SOLICITOR_PROFILE", PartyRole.CLAIMANT, "ORG123"))
            .contains("PCS:PCS:solicitor-org-claimant-access:claimant-solicitor:ORG123");
    }
}
