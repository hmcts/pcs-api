package uk.gov.hmcts.reform.pcs.ccd.util;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.sdk.type.CaseAccessGroup;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.GroupAccessType;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.ccd.entity.legalrepresentative.ClaimPartyLegalRepresentativeOrganisationEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.legalrepresentative.LegalRepresentativeOrganisationEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CaseAccessGroupsUtilTest {

    @Test
    void shouldDeriveSolicitorClaimantGroupIdForSolicitorProfileClaimant() {
        // given
        PartyEntity claimant = claimantEntity("J1XJ9VJ", "SOLICITOR_PROFILE");

        // when
        List<ListValue<CaseAccessGroup>> groups = CaseAccessGroupsUtil.deriveCaseAccessGroups(
            List.of(partyOf(claimant)), List.of(), Set.of(claimant));

        // then
        assertThat(groups).hasSize(1);
        CaseAccessGroup group = groups.getFirst().getValue();
        assertThat(group.getCaseAccessGroupId())
            .isEqualTo("PCS:PCS:solicitor-org-claimant-access:claimant-solicitor:J1XJ9VJ");
        assertThat(group.getCaseAccessGroupType()).isEqualTo(CaseAccessGroupsUtil.CCD_ALL_CASES_ACCESS);
    }

    @Test
    void shouldDeriveProfOrgClaimantGroupIdForLocalAuthorityClaimant() {
        // given
        PartyEntity claimant = claimantEntity("WK8GIHE", "LOCALAUTH_PROFILE");

        // when
        List<ListValue<CaseAccessGroup>> groups = CaseAccessGroupsUtil.deriveCaseAccessGroups(
            List.of(partyOf(claimant)), List.of(), Set.of(claimant));

        // then
        assertThat(groups).hasSize(1);
        assertThat(groups.getFirst().getValue().getCaseAccessGroupId())
            .isEqualTo("PCS:PCS:prof-org-claimant-access:claimant:WK8GIHE");
    }

    @Test
    void shouldDeriveDefendantGroupIdFromTheActiveLegalRepresentativeOrganisation() {
        // given a defendant represented by a solicitor organisation
        PartyEntity defendant = defendantEntity("ORG-DEF", "SOLICITOR_PROFILE", YesOrNo.YES);

        // when
        List<ListValue<CaseAccessGroup>> groups = CaseAccessGroupsUtil.deriveCaseAccessGroups(
            List.of(), List.of(partyOf(defendant)), Set.of(defendant));

        // then
        assertThat(groups).hasSize(1);
        assertThat(groups.getFirst().getValue().getCaseAccessGroupId())
            .isEqualTo("PCS:PCS:solicitor-org-defendant-access:defendant-solicitor:ORG-DEF");
    }

    @Test
    void shouldIgnoreDefendantsWhoseLegalRepresentativeLinkIsNoLongerActive() {
        // given a defendant whose representation has been revoked
        PartyEntity defendant = defendantEntity("ORG-DEF", "SOLICITOR_PROFILE", YesOrNo.NO);

        // when
        List<ListValue<CaseAccessGroup>> groups = CaseAccessGroupsUtil.deriveCaseAccessGroups(
            List.of(), List.of(partyOf(defendant)), Set.of(defendant));

        // then
        assertThat(groups).isEmpty();
    }

    @Test
    void shouldIgnoreDefendantsWithNoLegalRepresentativeOrganisationAtAll() {
        // given
        PartyEntity defendant = claimantEntity(null, null);

        // when
        List<ListValue<CaseAccessGroup>> groups = CaseAccessGroupsUtil.deriveCaseAccessGroups(
            List.of(), List.of(partyOf(defendant)), Set.of(defendant));

        // then
        assertThat(groups).isEmpty();
    }

    @Test
    void shouldDeriveGroupsForClaimantsAndDefendantsTogether() {
        // given
        PartyEntity claimant = claimantEntity("ORG-CLM", "LOCALAUTH_PROFILE");
        PartyEntity defendant = defendantEntity("ORG-DEF", "SOLICITOR_PROFILE", YesOrNo.YES);

        // when
        List<ListValue<CaseAccessGroup>> groups = CaseAccessGroupsUtil.deriveCaseAccessGroups(
            List.of(partyOf(claimant)), List.of(partyOf(defendant)), Set.of(claimant, defendant));

        // then
        assertThat(groups).extracting(listValue -> listValue.getValue().getCaseAccessGroupId())
            .containsExactly(
                "PCS:PCS:prof-org-claimant-access:claimant:ORG-CLM",
                "PCS:PCS:solicitor-org-defendant-access:defendant-solicitor:ORG-DEF");
    }

    @Test
    void shouldDeduplicateGroupsSharedByMoreThanOneParty() {
        // given two claimants belonging to the same organisation
        PartyEntity first = claimantEntity("ORG-SAME", "SOLICITOR_PROFILE");
        PartyEntity second = claimantEntity("ORG-SAME", "SOLICITOR_PROFILE");

        // when
        List<ListValue<CaseAccessGroup>> groups = CaseAccessGroupsUtil.deriveCaseAccessGroups(
            List.of(partyOf(first), partyOf(second)), List.of(), Set.of(first, second));

        // then
        assertThat(groups).hasSize(1);
    }

    @Test
    void shouldDeriveNothingWhenNoPartiesExistYet() {
        assertThat(CaseAccessGroupsUtil.deriveCaseAccessGroups(List.of(), List.of(), Set.of())).isEmpty();
    }

    @Test
    void shouldDeriveNothingWhenPartyCollectionsAreUnset() {
        // a case that has not captured its parties yet reads back null collections, not empty ones
        assertThat(CaseAccessGroupsUtil.deriveCaseAccessGroups(null, null, Set.of())).isEmpty();
    }

    @Test
    void shouldWrapGroupsAsCollectionItemsWithIds() {
        // given
        PartyEntity claimant = claimantEntity("ORG1", "SOLICITOR_PROFILE");

        // when
        List<ListValue<CaseAccessGroup>> groups = CaseAccessGroupsUtil.deriveCaseAccessGroups(
            List.of(partyOf(claimant)), List.of(), Set.of(claimant));

        // then - the data store's matcher silently skips collection items without an id
        assertThat(groups.getFirst().getId()).isNotBlank();
        assertThat(groups.getFirst().getValue()).isNotNull();
    }

    @Test
    void shouldDeriveTheSameListIdsAndOrderOnEveryRead() {
        // given
        PartyEntity first = claimantEntity("ORG-A", "SOLICITOR_PROFILE");
        PartyEntity second = claimantEntity("ORG-B", "LOCALAUTH_PROFILE");

        List<ListValue<Party>> claimants = List.of(partyOf(first), partyOf(second));
        Set<PartyEntity> entities = Set.of(first, second);

        // when
        List<ListValue<CaseAccessGroup>> firstRead =
            CaseAccessGroupsUtil.deriveCaseAccessGroups(claimants, List.of(), entities);
        List<ListValue<CaseAccessGroup>> secondRead =
            CaseAccessGroupsUtil.deriveCaseAccessGroups(claimants, List.of(), entities);

        // then
        assertThat(firstRead).usingRecursiveComparison().isEqualTo(secondRead);
        assertThat(firstRead).extracting(listValue -> listValue.getValue().getCaseAccessGroupId()).isSorted();
    }

    @Test
    void shouldResolveTheAccessTypeByProfileAndRoleRatherThanDeclarationOrder() {
        assertThat(GroupAccessType.forProfileAndRole("SOLICITOR_PROFILE", PartyRole.CLAIMANT, "ORG"))
            .isEqualTo("PCS:PCS:solicitor-org-claimant-access:claimant-solicitor:ORG");
        assertThat(GroupAccessType.forProfileAndRole("SOLICITOR_PROFILE", PartyRole.DEFENDANT, "ORG"))
            .isEqualTo("PCS:PCS:solicitor-org-defendant-access:defendant-solicitor:ORG");
        assertThat(GroupAccessType.forProfileAndRole("LOCALAUTH_PROFILE", PartyRole.CLAIMANT, "ORG"))
            .isEqualTo("PCS:PCS:prof-org-claimant-access:claimant:ORG");
    }

    @Test
    void shouldNeverIndexTheRequestBasedDutyAdvisorAccessType() {
        // duty-advisor access is requested per case, so it carries no party role and is never derived
        assertThat(GroupAccessType.DUTY_ADVISOR_ACCESS.getPartyRole()).isNull();

        assertThatThrownBy(() ->
            GroupAccessType.forProfileAndRole("SOLICITOR_PROFILE", PartyRole.UNDERLESSEE_OR_MORTGAGEE, "ORG"))
            .isInstanceOf(NullPointerException.class);
    }

    private ListValue<Party> partyOf(PartyEntity partyEntity) {
        return ListValue.<Party>builder()
            .value(Party.builder().id(partyEntity.getId().toString()).build())
            .id(partyEntity.getId().toString())
            .build();
    }

    private PartyEntity claimantEntity(String organisationId, String organisationProfileId) {
        return PartyEntity.builder()
            .id(UUID.randomUUID())
            .organisationId(organisationId)
            .organisationProfileId(organisationProfileId)
            .build();
    }

    private PartyEntity defendantEntity(String organisationId, String organisationProfileId, YesOrNo active) {
        PartyEntity defendant = PartyEntity.builder()
            .id(UUID.randomUUID())
            .claimPartyLegalRepresentativeOrganisationList(new ArrayList<>())
            .build();

        LegalRepresentativeOrganisationEntity legalRepOrganisation = LegalRepresentativeOrganisationEntity.builder()
            .organisationId(organisationId)
            .organisationProfileId(organisationProfileId)
            .build();

        defendant.getClaimPartyLegalRepresentativeOrganisationList().add(
            ClaimPartyLegalRepresentativeOrganisationEntity.builder()
                .party(defendant)
                .legalRepresentativeOrganisation(legalRepOrganisation)
                .active(active)
                .build());

        return defendant;
    }

}
