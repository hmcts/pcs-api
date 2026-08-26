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
            .forEach(accessType -> assertThat(GroupAccessType.caseAccessGroupIdTemplateFor(
                accessType.getOrganisationProfileId(), accessType.getPartyRole()))
                .contains(accessType.getCaseAccessGroupIdTemplate()));
    }

    @Test
    void shouldNotResolveATemplateForACombinationThatHasNoAccessType() {
        assertThat(GroupAccessType.caseAccessGroupIdTemplateFor(
            OrganisationProfile.LOCALAUTH_PROFILE.getId(), PartyRole.DEFENDANT)).isEmpty();
    }
}
