package uk.gov.hmcts.reform.pcs.ccd.util;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.sdk.type.CaseAccessGroup;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.GroupAccessType;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.ccd.entity.legalrepresentative.ClaimPartyOrganisationEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.legalrepresentative.OrganisationEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ClaimPartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;

import java.util.List;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class CaseAccessGroupsUtilTest {

    List<ListValue<Party>> defendants = List.of();

    @Test
    void shouldDeriveSolicitorGroupIdForSolicitorProfileParty() {
        Set<PartyEntity> parties = Set.of(party("J1XJ9VJ", "SOLICITOR_PROFILE"));


        List<ListValue<CaseAccessGroup>> groups = CaseAccessGroupsUtil.deriveCaseAccessGroups(parties, defendants);

        assertThat(groups).hasSize(1);
        CaseAccessGroup group = groups.getFirst().getValue();
        assertThat(group.getCaseAccessGroupId())
            .isEqualTo("PCS:PCS:solicitor-org-claimant-access:claimant-solicitor:J1XJ9VJ");
        assertThat(group.getCaseAccessGroupType()).isEqualTo("CCD:all-cases-access");
    }

    @Test
    void shouldDeriveProfOrgGroupIdForLocalAuthorityParty() {
        Set<PartyEntity> parties = Set.of(party("WK8GIHE", "LOCALAUTH_PROFILE"));

        List<ListValue<CaseAccessGroup>> groups = CaseAccessGroupsUtil.deriveCaseAccessGroups(parties, defendants);

        assertThat(groups).hasSize(1);
        assertThat(groups.getFirst().getValue().getCaseAccessGroupId())
            .isEqualTo("PCS:PCS:prof-org-claimant-access:claimant:WK8GIHE");
    }

    @Test
    void shouldDeriveNothingWhenNoPartiesExistYet() {
        assertThat(CaseAccessGroupsUtil.deriveCaseAccessGroups(Set.of(), defendants)).isEmpty();
    }

    @Test
    void shouldSkipPartiesWithoutAnOrganisation() {
        Set<PartyEntity> parties = Set.of(
            party(null, null, false),
            party("J1XJ9VJ", "SOLICITOR_PROFILE"));

        List<ListValue<CaseAccessGroup>> groups = CaseAccessGroupsUtil.deriveCaseAccessGroups(parties, defendants);

        assertThat(groups).hasSize(1);
        assertThat(groups.getFirst().getValue().getCaseAccessGroupId()).endsWith(":J1XJ9VJ");
    }

    @Test
    void shouldDeriveNothingForAnOrganisationPartyThatDidNotCreateTheClaim() {
        Set<PartyEntity> parties = Set.of(party("J1XJ9VJ", "SOLICITOR_PROFILE", false));

        assertThat(CaseAccessGroupsUtil.deriveCaseAccessGroups(parties, defendants)).isEmpty();
    }

    @Test
    void shouldKeepDerivingOnceTheClaimIsSubmittedAndTheClaimantHasARole() {
        PartyEntity claimant = party("J1XJ9VJ", "SOLICITOR_PROFILE");
        claimant.getClaimParties().add(claimPartyOf(claimant, PartyRole.CLAIMANT));

        assertThat(CaseAccessGroupsUtil.deriveCaseAccessGroups(Set.of(claimant), defendants))
            .extracting(lv -> lv.getValue().getCaseAccessGroupId())
            .containsExactly("PCS:PCS:solicitor-org-claimant-access:claimant-solicitor:J1XJ9VJ");
    }

    @Test
    void shouldDeriveFromTheClaimRoleWhenTheCreatorFlagWasNeverSet() {
        PartyEntity claimant = party("J1XJ9VJ", "SOLICITOR_PROFILE");
        claimant.getClaimParties().add(claimPartyOf(claimant, PartyRole.CLAIMANT));

        assertThat(CaseAccessGroupsUtil.deriveCaseAccessGroups(Set.of(claimant), defendants))
            .extracting(lv -> lv.getValue().getCaseAccessGroupId())
            .containsExactly("PCS:PCS:solicitor-org-claimant-access:claimant-solicitor:J1XJ9VJ");
    }

    private ClaimPartyEntity claimPartyOf(PartyEntity party, PartyRole role) {
        return ClaimPartyEntity.builder().party(party).role(role).build();
    }

    @Test
    void shouldDeriveNothingWhenPartyHasOrganisationButNoProfile() {
        Set<PartyEntity> parties = Set.of(party("J1XJ9VJ", null, false));

        assertThat(CaseAccessGroupsUtil.deriveCaseAccessGroups(parties, defendants)).isEmpty();
    }

    @Test
    void shouldWrapGroupsAsCollectionItemsWithIds() {
        Set<PartyEntity> parties = Set.of(party("ORG1", "SOLICITOR_PROFILE"));

        List<ListValue<CaseAccessGroup>> groups = CaseAccessGroupsUtil.deriveCaseAccessGroups(parties, defendants);

        // The data store's matcher silently skips collection items without an id.
        assertThat(groups.getFirst().getId()).isNotBlank();
        assertThat(groups.getFirst().getValue()).isNotNull();
    }

    private PartyEntity party(String organisationId, String organisationProfileId) {
        return party(organisationId, organisationProfileId, true);
    }

    private PartyEntity party(String organisationId, String organisationProfileId, boolean claimCreator) {
        return PartyEntity.builder()
            .id(UUID.randomUUID())
            .organisationId(organisationId)
            .organisationProfileId(organisationProfileId)
            .claimCreator(claimCreator)
            .build();
    }

    @Test
    void shouldDeriveTheSameListIdsAndOrderOnEveryRead() {
        // Given
        PartyEntity first = new PartyEntity();
        first.setId(UUID.randomUUID());
        first.setOrganisationId("ORG-A");
        first.setOrganisationProfileId("SOLICITOR_PROFILE");
        first.setClaimCreator(true);
        PartyEntity second = new PartyEntity();
        second.setId(UUID.randomUUID());
        second.setOrganisationId("ORG-B");
        second.setOrganisationProfileId("LOCALAUTH_PROFILE");
        second.setClaimCreator(true);

        // When
        List<ListValue<CaseAccessGroup>> firstRead =
            CaseAccessGroupsUtil.deriveCaseAccessGroups(new HashSet<>(Set.of(first, second)), defendants);
        List<ListValue<CaseAccessGroup>> secondRead =
            CaseAccessGroupsUtil.deriveCaseAccessGroups(new HashSet<>(Set.of(first, second)), defendants);

        // Then
        assertThat(firstRead).usingRecursiveComparison().isEqualTo(secondRead);
        assertThat(firstRead).extracting(lv -> lv.getValue().getCaseAccessGroupId()).isSorted();
    }


    @Test
    void shouldResolveTheClaimantAccessTypeByNameNotByDeclarationOrder() {
        assertThat(GroupAccessType.caseAccessGroupIdFor("SOLICITOR_PROFILE", PartyRole.CLAIMANT, "1234"))
            .isEqualTo("PCS:PCS:solicitor-org-claimant-access:claimant-solicitor:1234");
        assertThat(GroupAccessType.caseAccessGroupIdFor("SOLICITOR_PROFILE", PartyRole.DEFENDANT, "1234"))
            .isEqualTo("PCS:PCS:solicitor-org-defendant-access:defendant-solicitor:1234");
        assertThat(GroupAccessType.caseAccessGroupIdFor("LOCALAUTH_PROFILE", PartyRole.CLAIMANT, "1234"))
            .isEqualTo("PCS:PCS:prof-org-claimant-access:claimant:1234");
    }

    @Test
    void shouldNeverResolveTheRequestBasedDutyAdvisorAccessType() {
        assertThat(GroupAccessType.values())
            .filteredOn(type -> type == GroupAccessType.DUTY_ADVISOR_ACCESS)
            .allSatisfy(type -> assertThat(type.getPartyRole()).isNull());
    }

    @Test
    void shouldDeriveDefendantSolicitorGroupIdForDefendantWithLinkedOrg() {
        UUID partyId = UUID.randomUUID();
        PartyEntity defendant = PartyEntity.builder().id(partyId).build();
        defendant.getClaimPartyOrganisationList().add(activeOrg(defendant, "DEF_ORG", "SOLICITOR_PROFILE"));

        List<ListValue<Party>> defListValues = List.of(defendantListValue(partyId));

        List<ListValue<CaseAccessGroup>> groups =
            CaseAccessGroupsUtil.deriveCaseAccessGroups(Set.of(defendant), defListValues);

        assertThat(groups).hasSize(1);
        assertThat(groups.getFirst().getValue().getCaseAccessGroupId())
            .isEqualTo("PCS:PCS:solicitor-org-defendant-access:defendant-solicitor:DEF_ORG");
    }

    @Test
    void shouldNotDeriveDefendantGroupWhenNoActiveOrgLinked() {
        UUID partyId = UUID.randomUUID();
        PartyEntity defendant = PartyEntity.builder().id(partyId).build();

        List<ListValue<Party>> defListValues = List.of(defendantListValue(partyId));

        List<ListValue<CaseAccessGroup>> groups =
            CaseAccessGroupsUtil.deriveCaseAccessGroups(Set.of(defendant), defListValues);

        assertThat(groups).isEmpty();
    }

    @Test
    void shouldDeriveBothClaimantAndDefendantGroupsWhenBothPresent() {
        PartyEntity claimant = party("CLAIMANT_ORG", "SOLICITOR_PROFILE");

        UUID partyId = UUID.randomUUID();
        PartyEntity defendant = PartyEntity.builder().id(partyId).build();
        defendant.getClaimPartyOrganisationList().add(activeOrg(defendant, "DEF_ORG", "SOLICITOR_PROFILE"));

        List<ListValue<Party>> defListValues = List.of(defendantListValue(partyId));

        List<ListValue<CaseAccessGroup>> groups =
            CaseAccessGroupsUtil.deriveCaseAccessGroups(Set.of(claimant, defendant), defListValues);

        assertThat(groups).hasSize(2);
        assertThat(groups)
            .extracting(lv -> lv.getValue().getCaseAccessGroupId())
            .contains(
                "PCS:PCS:solicitor-org-claimant-access:claimant-solicitor:CLAIMANT_ORG",
                "PCS:PCS:solicitor-org-defendant-access:defendant-solicitor:DEF_ORG"
            );
    }

    @Test
    void shouldSkipDefendantWhosePartyIdIsNotInThePartiesSet() {
        UUID unknownPartyId = UUID.randomUUID();
        List<ListValue<Party>> defListValues = List.of(defendantListValue(unknownPartyId));

        // Should not throw NPE, and should return empty
        List<ListValue<CaseAccessGroup>> groups =
            CaseAccessGroupsUtil.deriveCaseAccessGroups(Set.of(), defListValues);

        assertThat(groups).isEmpty();
    }

    private ClaimPartyOrganisationEntity activeOrg(PartyEntity party, String orgId, String profileId) {
        OrganisationEntity org = OrganisationEntity.builder()
            .organisationId(orgId)
            .organisationProfileId(profileId)
            .build();
        return ClaimPartyOrganisationEntity.builder()
            .party(party)
            .organisation(org)
            .active(YesOrNo.YES)
            .build();
    }

    private ListValue<Party> defendantListValue(UUID partyId) {
        return ListValue.<Party>builder()
            .id(partyId.toString())
            .value(new Party())
            .build();
    }
}
