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
import uk.gov.hmcts.reform.pcs.ccd.repository.CounterClaimRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.GenAppRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.PartyRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.party.PartyService;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
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

    private RemovePartyService underTest;

    @BeforeEach
    void setUp() {
        underTest = new RemovePartyService(partyService, partyRepository, genAppRepository, counterClaimRepository);
    }

    @Test
    void shouldSoftDeletePartyAndDeactivateLegalRepresentativeLinks() {
        // Given
        PartyEntity partyToRemove = PartyEntity.builder().id(UUID.randomUUID()).active(YesOrNo.YES).build();
        PartyEntity remainingParty = PartyEntity.builder().id(UUID.randomUUID()).active(YesOrNo.YES).build();
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
        assertThat(partyToRemove.getActive()).isEqualTo(YesOrNo.NO);
        assertThat(legalRepresentativeLink.getActive()).isEqualTo(YesOrNo.NO);
        assertThat(result.partyName()).isEqualTo("Billy Wright");
        assertThat(result.partyRole()).isEqualTo(PartyRole.DEFENDANT);
        assertThat(result.partyLabel()).isEqualTo("Defendant 1");
        verify(partyRepository).save(partyToRemove);
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
        PartyEntity onlyDefendant = PartyEntity.builder().id(UUID.randomUUID()).active(YesOrNo.YES).build();
        buildCaseWithParties(onlyDefendant);

        when(partyService.getPartyRole(onlyDefendant)).thenReturn(PartyRole.DEFENDANT);
        when(partyService.isActive(onlyDefendant)).thenReturn(true);

        assertThatThrownBy(() -> underTest.validateCanRemove(onlyDefendant, TEST_CASE_REFERENCE))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage(LAST_PARTY_ERROR);
    }

    @Test
    void shouldRejectPartyWithOpenGeneralApplication() {
        PartyEntity partyToRemove = PartyEntity.builder().id(UUID.randomUUID()).active(YesOrNo.YES).build();
        PartyEntity remainingParty = PartyEntity.builder().id(UUID.randomUUID()).active(YesOrNo.YES).build();
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
        PartyEntity partyToRemove = PartyEntity.builder().id(UUID.randomUUID()).active(YesOrNo.YES).build();
        PartyEntity remainingParty = PartyEntity.builder().id(UUID.randomUUID()).active(YesOrNo.YES).build();
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
        PartyEntity partyToRemove = PartyEntity.builder().id(UUID.randomUUID()).active(YesOrNo.YES).build();
        PartyEntity remainingParty = PartyEntity.builder().id(UUID.randomUUID()).active(YesOrNo.YES).build();
        ClaimEntity mainClaim = buildCaseWithParties(partyToRemove, remainingParty);

        when(partyService.isActive(partyToRemove)).thenReturn(true);
        when(partyService.isActive(remainingParty)).thenReturn(true);

        assertThat(underTest.canSelectForRemoval(mainClaim.getClaimParties().getFirst(), mainClaim)).isTrue();
    }

    @Test
    void shouldNotAllowSelectingPartyWhenOnlyOneActivePartyHasSameRole() {
        PartyEntity partyToRemove = PartyEntity.builder().id(UUID.randomUUID()).active(YesOrNo.YES).build();
        PartyEntity inactiveParty = PartyEntity.builder().id(UUID.randomUUID()).active(YesOrNo.NO).build();
        ClaimEntity mainClaim = buildCaseWithParties(partyToRemove, inactiveParty);

        when(partyService.isActive(partyToRemove)).thenReturn(true);
        when(partyService.isActive(inactiveParty)).thenReturn(false);

        assertThat(underTest.canSelectForRemoval(mainClaim.getClaimParties().getFirst(), mainClaim)).isFalse();
    }

    private ClaimEntity buildCaseWithParties(PartyEntity... parties) {
        ClaimEntity mainClaim = ClaimEntity.builder().build();
        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder().claims(List.of(mainClaim)).build();
        mainClaim.setPcsCase(pcsCaseEntity);

        for (PartyEntity party : parties) {
            party.setPcsCase(pcsCaseEntity);
            mainClaim.addParty(party, PartyRole.DEFENDANT);
        }
        return mainClaim;
    }

    private DynamicList buildPartyList(UUID partyId) {
        return DynamicList.builder()
            .value(DynamicListElement.builder().code(partyId).label("Billy Wright - Defendant 1").build())
            .build();
    }
}
