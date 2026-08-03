package uk.gov.hmcts.reform.pcs.ccd.event.respondpossessionclaim.strategy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.YesNoNotSure;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.CounterClaimType;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.DefendantResponses;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.PossessionClaimResponse;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.CounterClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.DraftCaseDataService;
import uk.gov.hmcts.reform.pcs.ccd.service.party.PartyService;
import uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim.CounterClaimSubmitConfirmationService;
import uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim.RespondPossessionClaimSubmitPersistenceResult;
import uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim.RespondPossessionClaimSubmitService;
import uk.gov.hmcts.reform.pcs.ccd.util.SelectedPartyRetriever;
import uk.gov.hmcts.reform.pcs.exception.DraftNotFoundException;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;
import uk.gov.hmcts.reform.pcs.model.JourneyType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.respondPossessionClaim;

@ExtendWith(MockitoExtension.class)
class LegalRepSubmissionEventStrategyTest {

    private static final long CASE_REFERENCE = 1234567890L;
    private static final UUID USER_ID = UUID.randomUUID();

    @Mock
    private DraftCaseDataService draftCaseDataService;
    @Mock
    private SelectedPartyRetriever selectedPartyRetriever;
    @Mock
    private SubmitResponseFactory submitResponseFactory;
    @Mock
    private EventPayload<PCSCase, State> eventPayload;
    @Mock
    private SecurityContextService securityContextService;
    @Mock
    private PartyService partyService;
    @Mock
    private RespondPossessionClaimSubmitService respondPossessionClaimSubmitService;
    @Mock
    private CounterClaimSubmitConfirmationService counterClaimSubmitConfirmationService;

    private LegalRepSubmissionEventStrategy underTest;

    @BeforeEach
    void setUp() {
        underTest = new LegalRepSubmissionEventStrategy(
            draftCaseDataService,
            selectedPartyRetriever,
            submitResponseFactory,
            partyService,
            respondPossessionClaimSubmitService,
            counterClaimSubmitConfirmationService,
            securityContextService
        );
    }

    @Test
    void shouldSubmitLegalRepresentativeDraftForSelectedParty() {
        // given
        UUID representedPartyId = UUID.randomUUID();

        PartyEntity representedParty = PartyEntity.builder().id(representedPartyId).build();

        DefendantResponses responses = DefendantResponses.builder()
            .tenancyTypeConfirmation(YesNoNotSure.YES)
            .build();

        PossessionClaimResponse possessionClaimResponse = PossessionClaimResponse.builder()
            .defendantResponses(responses)
            .build();

        PCSCase caseData = PCSCase.builder()
            .possessionClaimResponse(possessionClaimResponse)
            .build();

        SubmitResponse<State> submitResponse = SubmitResponse.<State>builder()
            .build();

        CounterClaimEntity counterClaimEntity = CounterClaimEntity.builder()
            .claimType(CounterClaimType.SOMETHING_ELSE)
            .build();
        RespondPossessionClaimSubmitPersistenceResult persistenceResult =
            new RespondPossessionClaimSubmitPersistenceResult(
                possessionClaimResponse,
                counterClaimEntity,
                false
            );

        when(securityContextService.getCurrentUserId()).thenReturn(USER_ID);
        when(eventPayload.caseReference()).thenReturn(CASE_REFERENCE);
        when(eventPayload.caseData()).thenReturn(caseData);
        when(selectedPartyRetriever.getCurrentRepresentedPartyId(caseData)).thenReturn(Optional.of(representedPartyId));
        when(draftCaseDataService.getUnsubmittedCaseData(CASE_REFERENCE, respondPossessionClaim, representedPartyId))
            .thenReturn(Optional.of(caseData));
        when(partyService.getPartyEntityById(representedPartyId, CASE_REFERENCE)).thenReturn(representedParty);
        when(respondPossessionClaimSubmitService.persistFinalSubmit(
            CASE_REFERENCE,
            possessionClaimResponse,
            representedParty,
            JourneyType.LEGAL_REPRESENTATIVE)
        ).thenReturn(persistenceResult);
        when(counterClaimSubmitConfirmationService.buildSubmitResponse(CASE_REFERENCE, persistenceResult,
                                                                       representedParty)).thenReturn(submitResponse);
        // when
        SubmitResponse<State> result = underTest.process(eventPayload);

        // then
        assertThat(result.getErrors()).isNullOrEmpty();
        verify(respondPossessionClaimSubmitService).persistFinalSubmit(
            CASE_REFERENCE, possessionClaimResponse, representedParty, JourneyType.LEGAL_REPRESENTATIVE);
        verify(counterClaimSubmitConfirmationService)
            .buildSubmitResponse(CASE_REFERENCE, persistenceResult, representedParty);
    }

    @Test
    void shouldThrowExceptionForNoSelectedParty() {
        // given
        DefendantResponses responses = DefendantResponses.builder()
            .tenancyTypeConfirmation(YesNoNotSure.YES)
            .build();
        PossessionClaimResponse possessionClaimResponse = PossessionClaimResponse.builder()
            .defendantResponses(responses)
            .build();
        PCSCase caseData = PCSCase.builder()
            .possessionClaimResponse(possessionClaimResponse)
            .build();

        when(securityContextService.getCurrentUserId()).thenReturn(USER_ID);
        when(selectedPartyRetriever.getCurrentRepresentedPartyId(caseData)).thenReturn(Optional.empty());
        when(eventPayload.caseReference()).thenReturn(CASE_REFERENCE);
        when(eventPayload.caseData()).thenReturn(caseData);

        // when / then
        assertThat(assertThrows(
            IllegalStateException.class,
            () -> underTest.process(eventPayload)
        )).hasMessage("No selected responding party id for respond to claim");

        verify(respondPossessionClaimSubmitService, never()).persistFinalSubmit(anyLong(), any(), any(), any());
        verify(counterClaimSubmitConfirmationService, never()).buildSubmitResponse(anyLong(), any(), any());
    }

    @Test
    void shouldThrowWhenCurrentUserIdIsNull() {
        when(securityContextService.getCurrentUserId()).thenReturn(null);

        assertThat(assertThrows(
            IllegalStateException.class,
            () -> underTest.process(eventPayload)
        )).hasMessage("Current user IDAM ID is null");
    }

    @Test
    void shouldReturnValidationErrors() {
        // given
        UUID representedPartyId = UUID.randomUUID();

        DefendantResponses responses = DefendantResponses.builder()
            .tenancyTypeConfirmation(YesNoNotSure.YES)
            .build();

        PossessionClaimResponse possessionClaimResponse = PossessionClaimResponse.builder()
            .defendantResponses(responses)
            .build();

        PCSCase caseData = PCSCase.builder()
            .possessionClaimResponse(possessionClaimResponse)
            .build();

        SubmitResponse<State> submitResponse = SubmitResponse.<State>builder()
            .errors(List.of("error"))
            .build();

        when(securityContextService.getCurrentUserId()).thenReturn(USER_ID);
        when(selectedPartyRetriever.getCurrentRepresentedPartyId(caseData)).thenReturn(Optional.of(representedPartyId));
        when(draftCaseDataService.getUnsubmittedCaseData(CASE_REFERENCE, respondPossessionClaim, representedPartyId))
            .thenReturn(Optional.of(caseData));
        when(submitResponseFactory.validate(possessionClaimResponse, CASE_REFERENCE))
            .thenReturn(Optional.of(submitResponse));
        when(eventPayload.caseReference()).thenReturn(CASE_REFERENCE);
        when(eventPayload.caseData()).thenReturn(caseData);

        // when
        SubmitResponse<State> result = underTest.process(eventPayload);

        // then
        assertThat(result.getErrors()).contains("error");
        verify(respondPossessionClaimSubmitService, never()).persistFinalSubmit(anyLong(), any(), any(), any());
        verify(counterClaimSubmitConfirmationService, never()).buildSubmitResponse(anyLong(), any(), any());
    }

    @Test
    void shouldThrowExceptionWhenNoDraft() {
        // Given
        UUID representedPartyId = UUID.randomUUID();
        PCSCase caseData = PCSCase.builder()
            .build();

        // When
        when(securityContextService.getCurrentUserId()).thenReturn(USER_ID);
        when(selectedPartyRetriever.getCurrentRepresentedPartyId(caseData)).thenReturn(Optional.of(representedPartyId));
        when(eventPayload.caseReference()).thenReturn(CASE_REFERENCE);
        when(eventPayload.caseData()).thenReturn(caseData);

        // Then
        assertThatThrownBy(() -> underTest.process(eventPayload))
            .isInstanceOf(DraftNotFoundException.class);

        verify(respondPossessionClaimSubmitService, never()).persistFinalSubmit(anyLong(), any(), any(), any());
        verify(counterClaimSubmitConfirmationService, never()).buildSubmitResponse(anyLong(), any(), any());
    }

    @Test
    void supports_WithDefendantSolicitorUser_ReturnsTrue() {
        // when / then
        assertThat(underTest.supports(List.of(UserRole.DEFENDANT_SOLICITOR.getRole()))).isTrue();
    }

    @Test
    void supports_WithNonDefendantSolicitorUser_ReturnsFalse() {
        // when / then
        assertThat(underTest.supports(List.of(UserRole.CITIZEN.getRole()))).isFalse();
    }
}
