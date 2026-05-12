package uk.gov.hmcts.reform.pcs.ccd.event.respondpossessionclaim.strategy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.YesNoNotSure;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.DefendantResponses;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.PossessionClaimResponse;
import uk.gov.hmcts.reform.pcs.ccd.service.DraftCaseDataService;
import uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim.ClaimResponseService;
import uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim.DefendantResponseService;
import uk.gov.hmcts.reform.pcs.ccd.util.SelectedPartyRetriever;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.respondPossessionClaim;

@ExtendWith(MockitoExtension.class)
class LegalRepSubmissionEventStrategyTest {

    private static final long CASE_REFERENCE = 1234567890L;

    @Mock
    private DraftCaseDataService draftCaseDataService;
    @Mock
    private EventPayload<PCSCase, State> eventPayload;
    @Mock
    private ClaimResponseService claimResponseService;
    @Mock
    private DefendantResponseService defendantResponseService;
    @Mock
    private SelectedPartyRetriever selectedPartyRetriever;
    @Mock
    private SubmitResponseFactory submitResponseFactory;

    private LegalRepSubmissionEventStrategy underTest;

    @BeforeEach
    void setUp() {
        underTest = new LegalRepSubmissionEventStrategy(
            draftCaseDataService,
            claimResponseService,
            defendantResponseService,
            selectedPartyRetriever,
            submitResponseFactory
        );
    }

    @Test
    void shouldSubmitLegalRepresentativeDraftForSelectedParty() {
        // given
        UUID representedPartyId = UUID.randomUUID();

        DefendantResponses responses = DefendantResponses.builder()
            .tenancyTypeCorrect(YesNoNotSure.YES)
            .build();

        PossessionClaimResponse possessionClaimResponse = PossessionClaimResponse.builder()
            .defendantResponses(responses)
            .build();

        PCSCase caseData = PCSCase.builder()
            .possessionClaimResponse(possessionClaimResponse)
            .build();

        SubmitResponse<State> submitResponse = SubmitResponse.<State>builder()
            .build();

        when(selectedPartyRetriever.getSelectedPartyId(CASE_REFERENCE)).thenReturn(Optional.of(representedPartyId));
        when(draftCaseDataService.getUnsubmittedCaseData(CASE_REFERENCE, respondPossessionClaim, representedPartyId))
            .thenReturn(Optional.of(caseData));
        when(submitResponseFactory.success()).thenReturn(submitResponse);

        // when
        SubmitResponse<State> result = underTest.process(CASE_REFERENCE);

        // then
        assertThat(result.getErrors()).isNullOrEmpty();
        verify(claimResponseService).saveDraftDataForParty(possessionClaimResponse, CASE_REFERENCE, representedPartyId);
        verify(defendantResponseService).saveDefendantResponse(CASE_REFERENCE, possessionClaimResponse,
                                                               representedPartyId);
        verify(draftCaseDataService).deleteUnsubmittedCaseData(CASE_REFERENCE, respondPossessionClaim,
                                                               representedPartyId);
        verify(draftCaseDataService, never()).getUnsubmittedCaseData(CASE_REFERENCE, respondPossessionClaim);
    }

    @Test
    void shouldThrowExceptionForNoSelectedParty() {
        // given
        UUID representedPartyId = UUID.randomUUID();
        DefendantResponses responses = DefendantResponses.builder()
            .tenancyTypeCorrect(YesNoNotSure.YES)
            .build();

        PossessionClaimResponse possessionClaimResponse = PossessionClaimResponse.builder()
            .defendantResponses(responses)
            .build();

        when(selectedPartyRetriever.getSelectedPartyId(CASE_REFERENCE)).thenReturn(Optional.empty());

        // when / then
        assertThat(assertThrows(
            IllegalStateException.class,
            () -> underTest.process(CASE_REFERENCE)
        )).hasMessage("No selected responding party id for respond to claim");

        verify(claimResponseService, never())
            .saveDraftDataForParty(possessionClaimResponse, CASE_REFERENCE, representedPartyId);
        verify(defendantResponseService, never()).saveDefendantResponse(CASE_REFERENCE, possessionClaimResponse,
                                                                        representedPartyId);
        verify(draftCaseDataService, never()).deleteUnsubmittedCaseData(CASE_REFERENCE, respondPossessionClaim,
                                                                        representedPartyId);
        verify(draftCaseDataService, never()).getUnsubmittedCaseData(CASE_REFERENCE, respondPossessionClaim);
    }

    @Test
    void shouldReturnValidationErrors() {
        // given
        UUID representedPartyId = UUID.randomUUID();

        DefendantResponses responses = DefendantResponses.builder()
            .tenancyTypeCorrect(YesNoNotSure.YES)
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

        when(selectedPartyRetriever.getSelectedPartyId(CASE_REFERENCE)).thenReturn(Optional.of(representedPartyId));
        when(draftCaseDataService.getUnsubmittedCaseData(CASE_REFERENCE, respondPossessionClaim, representedPartyId))
            .thenReturn(Optional.of(caseData));
        when(submitResponseFactory.validate(possessionClaimResponse, CASE_REFERENCE)).thenReturn(submitResponse);

        // when
        SubmitResponse<State> result = underTest.process(CASE_REFERENCE);

        // then
        assertThat(result.getErrors()).contains("error");
        verify(claimResponseService, never()).saveDraftDataForParty(possessionClaimResponse, CASE_REFERENCE, representedPartyId);
        verify(defendantResponseService, never()).saveDefendantResponse(CASE_REFERENCE, possessionClaimResponse,
                                                               representedPartyId);
        verify(draftCaseDataService, never()).deleteUnsubmittedCaseData(CASE_REFERENCE, respondPossessionClaim,
                                                               representedPartyId);
        verify(draftCaseDataService, never()).getUnsubmittedCaseData(CASE_REFERENCE, respondPossessionClaim);
    }

    @Test
    void supports_ShouldReturnFalseWhenCitizen() {
        // when / then
        assertThat(underTest.supports(true)).isFalse();
    }

    @Test
    void supports_ShouldReturnTrueWhenNotCitizen() {
        // when / then
        assertThat(underTest.supports(false)).isTrue();
    }
}
