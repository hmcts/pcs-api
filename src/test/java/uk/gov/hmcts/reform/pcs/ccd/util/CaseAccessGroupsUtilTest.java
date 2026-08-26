package uk.gov.hmcts.reform.pcs.ccd.util;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.sdk.type.CaseAccessGroup;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.GroupAccessType;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ClaimPartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;

import java.util.List;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class CaseAccessGroupsUtilTest {

    @Test
    void shouldDeriveSolicitorGroupIdForSolicitorProfileParty() {
        Set<PartyEntity> parties = Set.of(party("J1XJ9VJ", "SOLICITOR_PROFILE"));

        List<ListValue<CaseAccessGroup>> groups = CaseAccessGroupsUtil.deriveCaseAccessGroups(parties);

        assertThat(groups).hasSize(1);
        CaseAccessGroup group = groups.getFirst().getValue();
        assertThat(group.getCaseAccessGroupId())
            .isEqualTo("PCS:PCS:solicitor-org-claimant-access:claimant-solicitor:J1XJ9VJ");
        assertThat(group.getCaseAccessGroupType()).isEqualTo("CCD:all-cases-access");
    }

    @Test
    void shouldDeriveProfOrgGroupIdForLocalAuthorityParty() {
        Set<PartyEntity> parties = Set.of(party("WK8GIHE", "LOCALAUTH_PROFILE"));

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
            party(null, null, false),
            party("J1XJ9VJ", "SOLICITOR_PROFILE"));

        List<ListValue<CaseAccessGroup>> groups = CaseAccessGroupsUtil.deriveCaseAccessGroups(parties);

        assertThat(groups).hasSize(1);
        assertThat(groups.getFirst().getValue().getCaseAccessGroupId()).endsWith(":J1XJ9VJ");
    }

    @Test
    void shouldDeriveNothingForAnOrganisationPartyThatDidNotCreateTheClaim() {
        Set<PartyEntity> parties = Set.of(party("J1XJ9VJ", "SOLICITOR_PROFILE", false));

        assertThat(CaseAccessGroupsUtil.deriveCaseAccessGroups(parties)).isEmpty();
    }

    @Test
    void shouldKeepDerivingOnceTheClaimIsSubmittedAndTheClaimantHasARole() {
        PartyEntity claimant = party("J1XJ9VJ", "SOLICITOR_PROFILE");
        claimant.getClaimParties().add(claimPartyOf(claimant, PartyRole.CLAIMANT));

        assertThat(CaseAccessGroupsUtil.deriveCaseAccessGroups(Set.of(claimant)))
            .extracting(lv -> lv.getValue().getCaseAccessGroupId())
            .containsExactly("PCS:PCS:solicitor-org-claimant-access:claimant-solicitor:J1XJ9VJ");
    }

    @Test
    void shouldDeriveFromTheClaimRoleWhenTheCreatorFlagWasNeverSet() {
        PartyEntity claimant = party("J1XJ9VJ", "SOLICITOR_PROFILE", false);
        claimant.getClaimParties().add(claimPartyOf(claimant, PartyRole.CLAIMANT));

        assertThat(CaseAccessGroupsUtil.deriveCaseAccessGroups(Set.of(claimant)))
            .extracting(lv -> lv.getValue().getCaseAccessGroupId())
            .containsExactly("PCS:PCS:solicitor-org-claimant-access:claimant-solicitor:J1XJ9VJ");
    }

    private ClaimPartyEntity claimPartyOf(PartyEntity party, PartyRole role) {
        return ClaimPartyEntity.builder().party(party).role(role).build();
    }

    @Test
    void shouldDeriveNothingWhenPartyHasOrganisationButNoProfile() {
        Set<PartyEntity> parties = Set.of(party("J1XJ9VJ", null));

        assertThat(CaseAccessGroupsUtil.deriveCaseAccessGroups(parties)).isEmpty();
    }

    @Test
    void shouldWrapGroupsAsCollectionItemsWithIds() {
        Set<PartyEntity> parties = Set.of(party("ORG1", "SOLICITOR_PROFILE"));

        List<ListValue<CaseAccessGroup>> groups = CaseAccessGroupsUtil.deriveCaseAccessGroups(parties);

        // The data store's matcher silently skips collection items without an id.
        assertThat(groups.getFirst().getId()).isNotBlank();
        assertThat(groups.getFirst().getValue()).isNotNull();
    }

    private PartyEntity party(String organisationId, String organisationProfileId) {
        return party(organisationId, organisationProfileId, true);
    }

    private PartyEntity party(String organisationId, String organisationProfileId, boolean claimCreator) {
        return PartyEntity.builder()
            .organisationId(organisationId)
            .organisationProfileId(organisationProfileId)
            .claimCreator(claimCreator)
            .build();
    }

    @Test
    void shouldDeriveTheSameListIdsAndOrderOnEveryRead() {
        // Given
        PartyEntity first = new PartyEntity();
        first.setOrganisationId("ORG-A");
        first.setOrganisationProfileId("SOLICITOR_PROFILE");
        first.setClaimCreator(true);
        PartyEntity second = new PartyEntity();
        second.setOrganisationId("ORG-B");
        second.setOrganisationProfileId("LOCALAUTH_PROFILE");
        second.setClaimCreator(true);

        // When
        List<ListValue<CaseAccessGroup>> firstRead =
            CaseAccessGroupsUtil.deriveCaseAccessGroups(new HashSet<>(Set.of(first, second)));
        List<ListValue<CaseAccessGroup>> secondRead =
            CaseAccessGroupsUtil.deriveCaseAccessGroups(new HashSet<>(Set.of(first, second)));

        // Then
        assertThat(firstRead).usingRecursiveComparison().isEqualTo(secondRead);
        assertThat(firstRead).extracting(lv -> lv.getValue().getCaseAccessGroupId()).isSorted();
    }


    @Test
    void shouldResolveTheClaimantAccessTypeByNameNotByDeclarationOrder() {
        assertThat(GroupAccessType.caseAccessGroupIdTemplateFor("SOLICITOR_PROFILE", PartyRole.CLAIMANT))
            .contains("PCS:PCS:solicitor-org-claimant-access:claimant-solicitor:$ORGID$");
        assertThat(GroupAccessType.caseAccessGroupIdTemplateFor("SOLICITOR_PROFILE", PartyRole.DEFENDANT))
            .contains("PCS:PCS:solicitor-org-defendant-access:defendant-solicitor:$ORGID$");
        assertThat(GroupAccessType.caseAccessGroupIdTemplateFor("LOCALAUTH_PROFILE", PartyRole.CLAIMANT))
            .contains("PCS:PCS:prof-org-claimant-access:claimant:$ORGID$");
    }

    @Test
    void shouldNeverResolveTheRequestBasedDutyAdvisorAccessType() {
        assertThat(GroupAccessType
                       .caseAccessGroupIdTemplateFor("SOLICITOR_PROFILE", PartyRole.UNDERLESSEE_OR_MORTGAGEE))
            .isEmpty();
        assertThat(GroupAccessType.values())
            .filteredOn(type -> type == GroupAccessType.DUTY_ADVISOR_ACCESS)
            .allSatisfy(type -> assertThat(type.getPartyRole()).isNull());
    }


}
