package uk.gov.hmcts.reform.pcs.ccd.service.caseworker.manageparty;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.DynamicList;
import uk.gov.hmcts.ccd.sdk.type.DynamicListElement;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.caseworker.RemovePartyDetails;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.legalrepresentative.ClaimPartyOrganisationEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ClaimPartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.CounterClaimRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.GenAppRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.PartyRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.party.PartyService;
import uk.gov.hmcts.reform.pcs.util.RevokeAccessHelper;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.ccd.service.caseworker.manageparty.RemovePartyService.LAST_PARTY_ERROR;
import static uk.gov.hmcts.reform.pcs.ccd.service.caseworker.manageparty.RemovePartyService.OPEN_APPLICATION_OR_COUNTERCLAIM_ERROR;
import static uk.gov.hmcts.reform.pcs.ccd.service.caseworker.manageparty.RemovePartyService.PARTY_CANNOT_BE_REMOVED_ERROR;

@ExtendWith(MockitoExtension.class)
class RemovePartyServiceTest {

    private static final long TEST_CASE_REFERENCE = 1234L;

    @Mock
    private PartyService partyService;
    @Mock
    private PartyRepository partyRepository;
    @Mock
    private GenAppRepository genAppRepository;
    @Mock
    private CounterClaimRepository counterClaimRepository;
    @Mock
    private RevokeAccessHelper revokeAccessHelper;

    private RemovePartyService underTest;

    @BeforeEach
    void setUp() {
        underTest = new RemovePartyService(
            partyService, partyRepository, genAppRepository, counterClaimRepository, revokeAccessHelper);
    }

    @Test
    void shouldSoftDeletePartyAndDeactivateLegalRepresentativeLinks() {
        // Given
        PartyEntity partyToRemove = PartyEntity.builder().id(UUID.randomUUID()).build();
        PartyEntity remainingParty = PartyEntity.builder().id(UUID.randomUUID()).build();
        ClaimPartyOrganisationEntity legalRepresentativeLink = ClaimPartyOrganisationEntity.builder()
            .active(YesOrNo.YES)
            .build();
        partyToRemove.getClaimPartyOrganisationList().add(legalRepresentativeLink);
        ClaimEntity mainClaim = buildCaseWithParties(partyToRemove, remainingParty);

        when(partyService.getPartyEntityById(partyToRemove.getId(), TEST_CASE_REFERENCE)).thenReturn(partyToRemove);
        when(partyService.getPartyRole(partyToRemove)).thenReturn(PartyRole.DEFENDANT);
        when(partyService.isActive(partyToRemove)).thenReturn(true);
        when(partyService.isActive(remainingParty)).thenReturn(true);
        when(partyService.getPartyLabel(mainClaim, partyToRemove.getId())).thenReturn("Defendant 1");
        when(partyService.getPartyName(partyToRemove)).thenReturn("Billy Wright");

        RemovePartyDetails removePartyDetails = RemovePartyDetails.builder()
            .partyToRemove(buildPartyList(partyToRemove.getId()))
            .removeSelectedParty(YesOrNo.YES)
            .build();

        // When
        RemovePartyService.RemovedParty result = underTest.removeParty(removePartyDetails, TEST_CASE_REFERENCE);

        // Then
        assertThat(partyToRemove.isRemoved()).isTrue();
        assertThat(legalRepresentativeLink.getActive()).isEqualTo(YesOrNo.NO);
        assertThat(result.partyName()).isEqualTo("Billy Wright");
        assertThat(result.partyRole()).isEqualTo(PartyRole.DEFENDANT);
        assertThat(result.partyLabel()).isEqualTo("Defendant 1");
        verify(partyRepository).save(partyToRemove);
    }

    @Test
    void shouldRevokeDefendantSelfRepresentationAccessWhenRemovingDefendant() {
        PartyEntity partyToRemove = PartyEntity.builder().id(UUID.randomUUID()).build();
        PartyEntity remainingParty = PartyEntity.builder().id(UUID.randomUUID()).build();
        ClaimEntity mainClaim = buildCaseWithParties(partyToRemove, remainingParty);

        when(partyService.getPartyEntityById(partyToRemove.getId(), TEST_CASE_REFERENCE)).thenReturn(partyToRemove);
        when(partyService.getPartyRole(partyToRemove)).thenReturn(PartyRole.DEFENDANT);
        when(partyService.isActive(partyToRemove)).thenReturn(true);
        when(partyService.isActive(remainingParty)).thenReturn(true);
        when(partyService.getPartyLabel(mainClaim, partyToRemove.getId())).thenReturn("Defendant 1");

        RemovePartyDetails removePartyDetails = RemovePartyDetails.builder()
            .partyToRemove(buildPartyList(partyToRemove.getId()))
            .removeSelectedParty(YesOrNo.YES)
            .build();

        underTest.removeParty(removePartyDetails, TEST_CASE_REFERENCE);

        verify(revokeAccessHelper).closeDefendantsSelfRepresentation(partyToRemove.getPcsCase(), partyToRemove);
    }

    @Test
    void shouldNotRevokeDefendantSelfRepresentationAccessWhenRemovingClaimant() {
        PartyEntity partyToRemove = PartyEntity.builder().id(UUID.randomUUID()).build();
        PartyEntity remainingClaimant = PartyEntity.builder().id(UUID.randomUUID()).build();
        ClaimEntity mainClaim = buildCaseWithPartiesByRole(
            List.of(partyWithRole(partyToRemove, PartyRole.CLAIMANT),
                    partyWithRole(remainingClaimant, PartyRole.CLAIMANT)));

        when(partyService.getPartyEntityById(partyToRemove.getId(), TEST_CASE_REFERENCE)).thenReturn(partyToRemove);
        when(partyService.getPartyRole(partyToRemove)).thenReturn(PartyRole.CLAIMANT);
        when(partyService.isActive(partyToRemove)).thenReturn(true);
        when(partyService.isActive(remainingClaimant)).thenReturn(true);
        when(partyService.getPartyLabel(mainClaim, partyToRemove.getId())).thenReturn("Claimant 1");

        RemovePartyDetails removePartyDetails = RemovePartyDetails.builder()
            .partyToRemove(buildPartyList(partyToRemove.getId()))
            .removeSelectedParty(YesOrNo.YES)
            .build();

        underTest.removeParty(removePartyDetails, TEST_CASE_REFERENCE);

        verify(revokeAccessHelper, never()).closeDefendantsSelfRepresentation(partyToRemove.getPcsCase(),
                                                                              partyToRemove);
    }

    @Test
    void shouldRejectNoConfirmation() {
        RemovePartyDetails removePartyDetails = RemovePartyDetails.builder()
            .removeSelectedParty(YesOrNo.NO)
            .build();

        assertThatThrownBy(() -> underTest.removeParty(removePartyDetails, TEST_CASE_REFERENCE))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage(PARTY_CANNOT_BE_REMOVED_ERROR);
    }

    @Test
    void shouldRejectLastPartyOfRole() {
        PartyEntity onlyDefendant = PartyEntity.builder().id(UUID.randomUUID()).build();
        buildCaseWithParties(onlyDefendant);

        when(partyService.getPartyRole(onlyDefendant)).thenReturn(PartyRole.DEFENDANT);
        when(partyService.isActive(onlyDefendant)).thenReturn(true);

        assertThatThrownBy(() -> underTest.validateCanRemove(onlyDefendant, TEST_CASE_REFERENCE))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage(LAST_PARTY_ERROR);
    }

    @Test
    void shouldRejectPartyWithOpenGeneralApplication() {
        PartyEntity partyToRemove = PartyEntity.builder().id(UUID.randomUUID()).build();
        PartyEntity remainingParty = PartyEntity.builder().id(UUID.randomUUID()).build();
        buildCaseWithParties(partyToRemove, remainingParty);

        when(partyService.getPartyRole(partyToRemove)).thenReturn(PartyRole.DEFENDANT);
        when(partyService.isActive(partyToRemove)).thenReturn(true);
        when(partyService.isActive(remainingParty)).thenReturn(true);
        when(genAppRepository.existsByPcsCaseCaseReferenceAndPartyIdAndStateIn(
            eq(TEST_CASE_REFERENCE), eq(partyToRemove.getId()), anyCollection())).thenReturn(true);

        assertThatThrownBy(() -> underTest.validateCanRemove(partyToRemove, TEST_CASE_REFERENCE))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage(OPEN_APPLICATION_OR_COUNTERCLAIM_ERROR);
    }

    @Test
    void shouldRejectPartyWithOpenCounterClaim() {
        PartyEntity partyToRemove = PartyEntity.builder().id(UUID.randomUUID()).build();
        PartyEntity remainingParty = PartyEntity.builder().id(UUID.randomUUID()).build();
        buildCaseWithParties(partyToRemove, remainingParty);

        when(partyService.getPartyRole(partyToRemove)).thenReturn(PartyRole.DEFENDANT);
        when(partyService.isActive(partyToRemove)).thenReturn(true);
        when(partyService.isActive(remainingParty)).thenReturn(true);
        when(counterClaimRepository.existsByPcsCaseCaseReferenceAndPartyIdAndStatusIn(
            eq(TEST_CASE_REFERENCE), eq(partyToRemove.getId()), anyCollection())).thenReturn(true);

        assertThatThrownBy(() -> underTest.validateCanRemove(partyToRemove, TEST_CASE_REFERENCE))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage(OPEN_APPLICATION_OR_COUNTERCLAIM_ERROR);
    }

    @Test
    void shouldAllowSelectingPartyWhenMoreThanOneActivePartyHasSameRole() {
        PartyEntity partyToRemove = PartyEntity.builder().id(UUID.randomUUID()).build();
        PartyEntity remainingParty = PartyEntity.builder().id(UUID.randomUUID()).build();
        ClaimEntity mainClaim = buildCaseWithParties(partyToRemove, remainingParty);

        when(partyService.isActive(partyToRemove)).thenReturn(true);
        when(partyService.isActive(remainingParty)).thenReturn(true);

        assertThat(underTest.canSelectForRemoval(mainClaim.getClaimParties().getFirst(), mainClaim)).isTrue();
    }

    @Test
    void shouldNotAllowSelectingPartyWhenOnlyOneActivePartyHasSameRole() {
        PartyEntity partyToRemove = PartyEntity.builder().id(UUID.randomUUID()).build();
        PartyEntity inactiveParty = PartyEntity.builder().id(UUID.randomUUID()).removed(true).build();
        ClaimEntity mainClaim = buildCaseWithParties(partyToRemove, inactiveParty);

        when(partyService.isActive(partyToRemove)).thenReturn(true);
        when(partyService.isActive(inactiveParty)).thenReturn(false);

        assertThat(underTest.canSelectForRemoval(mainClaim.getClaimParties().getFirst(), mainClaim)).isFalse();
    }

    @Test
    void shouldFindAnyRemovableParty() {
        PartyEntity partyToRemove = PartyEntity.builder().id(UUID.randomUUID()).build();
        PartyEntity remainingParty = PartyEntity.builder().id(UUID.randomUUID()).build();
        ClaimEntity mainClaim = buildCaseWithParties(partyToRemove, remainingParty);

        when(partyService.isActive(partyToRemove)).thenReturn(true);
        when(partyService.isActive(remainingParty)).thenReturn(true);

        assertThat(underTest.hasAnyRemovableParty(mainClaim)).isTrue();
    }

    @Test
    void shouldFilterActiveClaimantsAndDefendants() {
        PartyEntity claimant = PartyEntity.builder().id(UUID.randomUUID()).build();
        PartyEntity defendant = PartyEntity.builder().id(UUID.randomUUID()).build();
        PartyEntity litigationFriend = PartyEntity.builder().id(UUID.randomUUID()).build();
        PartyEntity inactiveDefendant = PartyEntity.builder().id(UUID.randomUUID()).removed(true).build();
        ClaimEntity mainClaim = ClaimEntity.builder().build();
        mainClaim.addParty(claimant, PartyRole.CLAIMANT);
        mainClaim.addParty(defendant, PartyRole.DEFENDANT);
        mainClaim.addParty(litigationFriend, PartyRole.LITIGATION_FRIEND);
        mainClaim.addParty(inactiveDefendant, PartyRole.DEFENDANT);

        when(partyService.isActive(claimant)).thenReturn(true);
        when(partyService.isActive(defendant)).thenReturn(true);
        when(partyService.isActive(inactiveDefendant)).thenReturn(false);

        assertThat(underTest.getActiveClaimantsAndDefendants(mainClaim))
            .extracting(ClaimPartyEntity::getParty)
            .containsExactly(claimant, defendant);
    }

    private ClaimEntity buildCaseWithParties(PartyEntity... parties) {
        return buildCaseWithPartiesByRole(
            java.util.Arrays.stream(parties)
                .map(party -> partyWithRole(party, PartyRole.DEFENDANT))
                .toList());
    }

    private ClaimEntity buildCaseWithPartiesByRole(List<PartyWithRole> parties) {
        ClaimEntity mainClaim = ClaimEntity.builder().build();
        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder().claims(List.of(mainClaim)).build();
        mainClaim.setPcsCase(pcsCaseEntity);

        for (PartyWithRole partyWithRole : parties) {
            partyWithRole.party().setPcsCase(pcsCaseEntity);
            mainClaim.addParty(partyWithRole.party(), partyWithRole.role());
        }
        return mainClaim;
    }

    private PartyWithRole partyWithRole(PartyEntity party, PartyRole role) {
        return new PartyWithRole(party, role);
    }

    private DynamicList buildPartyList(UUID partyId) {
        return DynamicList.builder()
            .value(DynamicListElement.builder().code(partyId).label("Billy Wright - Defendant 1").build())
            .build();
    }

    private record PartyWithRole(PartyEntity party, PartyRole role) {
    }
}
