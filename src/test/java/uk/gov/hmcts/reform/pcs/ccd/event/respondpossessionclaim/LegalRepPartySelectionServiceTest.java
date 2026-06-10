package uk.gov.hmcts.reform.pcs.ccd.event.respondpossessionclaim;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.DefendantContactDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.PossessionClaimResponse;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.event.respondpossessionclaim.utils.ClaimantOrgNameListCreator;
import uk.gov.hmcts.reform.pcs.ccd.event.respondpossessionclaim.utils.PossessionClaimMerger;
import uk.gov.hmcts.reform.pcs.ccd.repository.DefendantResponseRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.DraftCaseDataService;
import uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim.PossessionClaimResponseMapper;
import uk.gov.hmcts.reform.pcs.ccd.util.SelectedPartyRetriever;
import uk.gov.hmcts.reform.pcs.exception.CaseAccessException;
import uk.gov.hmcts.reform.pcs.exception.DraftNotFoundException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.respondPossessionClaim;

@ExtendWith(MockitoExtension.class)
class LegalRepPartySelectionServiceTest {

    @Mock
    private SelectedPartyRetriever selectedPartyRetriever;

    @Mock
    private DefendantResponseRepository defendantResponseRepository;

    @Mock
    private DraftCaseDataService draftCaseDataService;

    @Mock
    private PossessionClaimResponseMapper responseMapper;

    private LegalRepPartySelectionService underTest;

    @BeforeEach
    void setUp() {
        underTest = new LegalRepPartySelectionService(selectedPartyRetriever,
                                                    defendantResponseRepository,
                                                    draftCaseDataService,
                                                    responseMapper,
                                                      new PossessionClaimMerger(new ClaimantOrgNameListCreator()));
    }

    @Test
    void shouldBuildCaseWithRepresentedPartiesOnlyWhenNoSelectedParty() {
        // given
        PCSCase pcsCase = PCSCase.builder()
            .build();

        UUID partyId = UUID.randomUUID();
        PartyEntity partyEntity = PartyEntity.builder()
            .id(partyId)
            .firstName("John")
            .lastName("Smith")
            .orgName("Org")
            .build();

        UUID organisationId = UUID.randomUUID();

        when(selectedPartyRetriever.getSelectedPartyId(pcsCase)).thenReturn(Optional.empty());

        // when
        PCSCase result = underTest.getDraft(pcsCase, List.of(partyEntity), 12345L, organisationId);

        // then
        assertThat(result.getParties()).hasSize(1);
        assertThat(result.getAllLinkedDefendants()).hasSize(1);
        assertThat(result.getPossessionClaimResponse()).isNull();
        assertThat(result.getHasUnsubmittedCaseData()).isNull();
        assertThat(result.getParties().getFirst().getId()).isEqualTo(partyId.toString());
        assertThat(result.getParties().getFirst().getValue().getFirstName()).isEqualTo("John");
    }

    @Test
    void shouldThrowExceptionWhenResponseAlreadySubmitted() {
        // given
        PCSCase pcsCase = PCSCase.builder()
            .build();

        UUID partyId = UUID.randomUUID();
        PartyEntity partyEntity = PartyEntity.builder()
            .id(partyId)
            .build();
        UUID organisationId = UUID.randomUUID();

        when(selectedPartyRetriever.getSelectedPartyId(pcsCase)).thenReturn(Optional.of(partyId));
        when(defendantResponseRepository.existsByClaimPcsCaseCaseReferenceAndPartyId(12345L, partyId)).thenReturn(true);

        // when / then
        assertThatThrownBy(() -> underTest.getDraft(pcsCase, List.of(partyEntity), 12345L, organisationId)).isInstanceOf(
            IllegalStateException.class).hasMessage("A response has already been submitted for this case.");
    }

    @Test
    void shouldInitialiseDraftWhenNoExistingDraft() {
        // given
        long caseReference = 12345L;
        UUID partyId = UUID.randomUUID();

        PCSCase pcsCase = PCSCase.builder()
            .build();

        PartyEntity partyEntity = PartyEntity.builder()
            .id(partyId)
            .build();

        DefendantContactDetails contactDetails = DefendantContactDetails.builder()
            .build();

        PossessionClaimResponse response = PossessionClaimResponse.builder()
            .defendantContactDetails(contactDetails)
            .build();
        UUID organisationId = UUID.randomUUID();

        when(selectedPartyRetriever.getSelectedPartyId(pcsCase)).thenReturn(Optional.of(partyId));
        when(defendantResponseRepository.existsByClaimPcsCaseCaseReferenceAndPartyId(
            caseReference,
            partyId
        )).thenReturn(false);
        when(draftCaseDataService.hasUnsubmittedCaseData(caseReference, respondPossessionClaim, partyId, organisationId)).thenReturn(
            false);
        when(responseMapper.mapFrom(pcsCase, partyEntity)).thenReturn(response);

        // when
        PCSCase result = underTest.getDraft(pcsCase, List.of(partyEntity), caseReference, organisationId);

        // then
        assertThat(result.getPossessionClaimResponse()).isEqualTo(response);

        ArgumentCaptor<PCSCase> captor = ArgumentCaptor.forClass(PCSCase.class);

        verify(draftCaseDataService).patchUnsubmittedEventData(
            eq(caseReference),
            captor.capture(),
            eq(respondPossessionClaim),
            eq(partyId),
            eq(organisationId)
        );

        PCSCase savedDraft = captor.getValue();

        assertThat(savedDraft.getPossessionClaimResponse().getDefendantContactDetails()).isEqualTo(contactDetails);
    }

    @Test
    void shouldRestoreExistingDraft() {
        // given
        long caseReference = 12345L;
        UUID partyId = UUID.randomUUID();

        PartyEntity partyEntity = PartyEntity.builder()
            .id(partyId)
            .build();

        Party defendantParty = Party.builder()
            .firstName("Defendant")
            .build();
        List<ListValue<Party>> claimantOrg = List.of(ListValue.<Party>builder()
                                                         .id("1")
                                                         .value(Party.builder()
                                                                    .orgName("Claimant Org")
                                                                    .build())
                                                         .build());
        PCSCase pcsCase = PCSCase.builder()
            .allClaimants(claimantOrg)
            .allDefendants(List.of(ListValue.<Party>builder()
                                       .id(partyId.toString())
                                       .value(defendantParty)
                                       .build()))
            .build();

        PossessionClaimResponse savedResponse = PossessionClaimResponse.builder()
            .build();

        PCSCase savedDraft = PCSCase.builder()
            .possessionClaimResponse(savedResponse)
            .build();
        UUID organisationId = UUID.randomUUID();

        when(selectedPartyRetriever.getSelectedPartyId(pcsCase)).thenReturn(Optional.of(partyId));
        when(defendantResponseRepository.existsByClaimPcsCaseCaseReferenceAndPartyId(
            caseReference,
            partyId
        )).thenReturn(false);
        when(draftCaseDataService.hasUnsubmittedCaseData(caseReference, respondPossessionClaim, partyId, organisationId)).thenReturn(
            true);
        when(draftCaseDataService.getUnsubmittedCaseData(caseReference, respondPossessionClaim, partyId, organisationId)).thenReturn(
            Optional.of(savedDraft));
        when(responseMapper.buildPartyFromEntity(partyEntity, pcsCase)).thenReturn(defendantParty);

        // when
        PCSCase result = underTest.getDraft(pcsCase, List.of(partyEntity), caseReference, organisationId);

        // then
        assertThat(result.getHasUnsubmittedCaseData()).isEqualTo(YesOrNo.YES);
        assertThat(result.getPossessionClaimResponse().getClaimantEnteredDefendantDetails()).isEqualTo(defendantParty);
        assertThat(result.getAllLinkedDefendants()).hasSize(1);
    }

    @Test
    void shouldThrowCaseAccessExceptionWhenPartyNotMatched() {
        // given
        PCSCase pcsCase = PCSCase.builder()
            .build();

        UUID selectedPartyId = UUID.randomUUID();
        UUID organisationId = UUID.randomUUID();

        when(selectedPartyRetriever.getSelectedPartyId(pcsCase)).thenReturn(Optional.of(selectedPartyId));

        // when / then
        assertThatThrownBy(() -> underTest.getDraft(
            pcsCase,
            List.of(),
            12345L,
            organisationId
        )).isInstanceOf(CaseAccessException.class).hasMessage("User is not linked as a defendant on this case");
    }

    @Test
    void shouldThrowDraftNotFoundException() {
        // given
        long caseReference = 12345L;
        UUID partyId = UUID.randomUUID();

        PCSCase pcsCase = PCSCase.builder()
            .build();

        PartyEntity partyEntity = PartyEntity.builder()
            .id(partyId)
            .build();
        UUID organisationId = UUID.randomUUID();

        when(selectedPartyRetriever.getSelectedPartyId(pcsCase)).thenReturn(Optional.of(partyId));
        when(defendantResponseRepository.existsByClaimPcsCaseCaseReferenceAndPartyId(
            caseReference,
            partyId
        )).thenReturn(false);
        when(draftCaseDataService.hasUnsubmittedCaseData(caseReference, respondPossessionClaim, partyId, organisationId)).thenReturn(
            true);
        when(draftCaseDataService.getUnsubmittedCaseData(caseReference, respondPossessionClaim, partyId, organisationId)).thenReturn(
            Optional.empty());

        // when / then
        assertThatThrownBy(() -> underTest.getDraft(pcsCase, List.of(partyEntity), caseReference, organisationId)).isInstanceOf(
            DraftNotFoundException.class);
    }

    @Test
    void shouldSetLinkedDefendantsToMatchedDefendant() {

        long caseReference = 12345L;
        UUID matchedPartyId = UUID.randomUUID();
        UUID otherPartyId = UUID.randomUUID();

        PartyEntity matchedPartyEntity = PartyEntity.builder().id(matchedPartyId).build();

        Party matchedParty = Party.builder().firstName("Matched").build();

        Party otherParty = Party.builder().firstName("Other").build();

        PCSCase pcsCase = PCSCase.builder().allClaimants(List.of(ListValue.<Party>builder()
                                                                     .id("1")
                                                                     .value(Party.builder()
                                                                                .orgName("Claimant Org")
                                                                                .build())
                                                                     .build()))
            .allDefendants(List.of(
            ListValue.<Party>builder().id(matchedPartyId.toString()).value(matchedParty).build(),
            ListValue.<Party>builder().id(otherPartyId.toString()).value(otherParty).build()
        )).build();

        PossessionClaimResponse savedResponse = PossessionClaimResponse.builder().build();

        PCSCase savedDraft = PCSCase.builder().possessionClaimResponse(savedResponse).build();
        UUID organisationId = UUID.randomUUID();

        when(draftCaseDataService.hasUnsubmittedCaseData(
            caseReference,
            respondPossessionClaim,
            matchedPartyId,
            organisationId
        )).thenReturn(true);

        when(draftCaseDataService.getUnsubmittedCaseData(
            caseReference,
            respondPossessionClaim,
            matchedPartyId,
            organisationId
        )).thenReturn(Optional.of(savedDraft));

        when(responseMapper.buildPartyFromEntity(matchedPartyEntity, pcsCase)).thenReturn(matchedParty);
        PartyEntity defendant1 = PartyEntity.builder().id(matchedPartyId).build();

        List<PartyEntity> defendants = List.of(defendant1);

        PCSCase result = underTest.getDraftCaseData(caseReference, pcsCase, matchedPartyEntity, defendants, organisationId);

        assertThat(result.getAllLinkedDefendants()).hasSize(1);
        assertThat(result.getAllLinkedDefendants().getFirst().getId()).isEqualTo(matchedPartyId.toString());
        assertThat(result.getHasUnsubmittedCaseData()).isEqualTo(YesOrNo.YES);
    }
}
