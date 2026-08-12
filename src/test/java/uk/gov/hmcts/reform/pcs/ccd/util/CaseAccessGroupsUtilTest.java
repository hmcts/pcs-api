package uk.gov.hmcts.reform.pcs.ccd.util;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.sdk.type.CaseAccessGroup;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;

import java.util.List;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CaseAccessGroupsUtilTest {

    @Test
    void shouldDeriveSolicitorGroupIdForSolicitorProfileParty() {
        Set<PartyEntity> parties = Set.of(party("J1XJ9VJ", List.of("SOLICITOR_PROFILE", "ORGANISATION_PROFILE")));

        List<ListValue<CaseAccessGroup>> groups = CaseAccessGroupsUtil.deriveCaseAccessGroups(parties);

        assertThat(groups).hasSize(1);
        CaseAccessGroup group = groups.getFirst().getValue();
        assertThat(group.getCaseAccessGroupId())
            .isEqualTo("PCS:PCS:solicitor-org-claimant-access:claimant-solicitor:J1XJ9VJ");
        assertThat(group.getCaseAccessGroupType()).isEqualTo("CCD:all-cases-access");
    }

    @Test
    void shouldDeriveProfOrgGroupIdForLocalAuthorityParty() {
        Set<PartyEntity> parties = Set.of(party("WK8GIHE", List.of("LOCALAUTH_PROFILE", "ORGANISATION_PROFILE")));

        List<ListValue<CaseAccessGroup>> groups = CaseAccessGroupsUtil.deriveCaseAccessGroups(parties);

        assertThat(groups).hasSize(1);
        assertThat(groups.getFirst().getValue().getCaseAccessGroupId())
            .isEqualTo("PCS:PCS:prof-org-claimant-access:claimant:WK8GIHE");
    }

    @Test
    void shouldDeriveNothingWhenNoPartiesExistYet() {
        assertThat(CaseAccessGroupsUtil.deriveCaseAccessGroups(Set.of())).isEmpty();
    }

    @Test
    void shouldSkipPartiesWithoutAnOrganisation() {
        Set<PartyEntity> parties = Set.of(
            party(null, null),
            party("J1XJ9VJ", List.of("SOLICITOR_PROFILE")));

        List<ListValue<CaseAccessGroup>> groups = CaseAccessGroupsUtil.deriveCaseAccessGroups(parties);

        assertThat(groups).hasSize(1);
        assertThat(groups.getFirst().getValue().getCaseAccessGroupId()).endsWith(":J1XJ9VJ");
    }

    @Test
    void shouldThrowWhenPartyHasOrganisationButNoProfiles() {
        Set<PartyEntity> parties = Set.of(party("J1XJ9VJ", null));

        assertThatThrownBy(() -> CaseAccessGroupsUtil.deriveCaseAccessGroups(parties))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldWrapGroupsAsCollectionItemsWithIds() {
        Set<PartyEntity> parties = Set.of(party("ORG1", List.of("SOLICITOR_PROFILE")));

        List<ListValue<CaseAccessGroup>> groups = CaseAccessGroupsUtil.deriveCaseAccessGroups(parties);

        // The data store's matcher silently skips collection items without an id.
        assertThat(groups.getFirst().getId()).isNotBlank();
        assertThat(groups.getFirst().getValue()).isNotNull();
    }

    private PartyEntity party(String organisationId, List<String> organisationProfileIds) {
        return PartyEntity.builder()
            .organisationId(organisationId)
            .organisationProfileIds(organisationProfileIds)
            .build();
    }

    @Test
    void shouldDeriveTheSameListIdsAndOrderOnEveryRead() {
        // Given two organisation-owned claimant parties, held in a HashSet and recomputed per read
        PartyEntity first = new PartyEntity();
        first.setOrganisationId("ORG-A");
        first.setOrganisationProfileIds(List.of("SOLICITOR_PROFILE"));
        PartyEntity second = new PartyEntity();
        second.setOrganisationId("ORG-B");
        second.setOrganisationProfileIds(List.of("LOCALAUTH_PROFILE"));

        // When derived twice
        List<ListValue<CaseAccessGroup>> firstRead =
            CaseAccessGroupsUtil.deriveCaseAccessGroups(new HashSet<>(Set.of(first, second)));
        List<ListValue<CaseAccessGroup>> secondRead =
            CaseAccessGroupsUtil.deriveCaseAccessGroups(new HashSet<>(Set.of(first, second)));

        // Then an unchanged case produces an identical payload, ids and order included
        assertThat(firstRead).usingRecursiveComparison().isEqualTo(secondRead);
        assertThat(firstRead).extracting(lv -> lv.getValue().getCaseAccessGroupId()).isSorted();
    }

}
