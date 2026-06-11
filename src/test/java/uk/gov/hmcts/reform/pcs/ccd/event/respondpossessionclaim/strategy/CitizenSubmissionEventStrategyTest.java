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
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.DefendantResponses;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.PossessionClaimResponse;
import uk.gov.hmcts.reform.pcs.ccd.service.DraftCaseDataService;
import uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim.CounterClaimSubmitConfirmationService;
import uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim.RespondPossessionClaimSubmitPersistenceResult;
import uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim.RespondPossessionClaimSubmitService;
import uk.gov.hmcts.reform.pcs.exception.DraftNotFoundException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.respondPossessionClaim;

@ExtendWith(MockitoExtension.class)
class CitizenSubmissionEventStrategyTest {

    private static final long CASE_REFERENCE = 1234567890L;

    @Mock
    private DraftCaseDataService draftCaseDataService;
    @Mock
    private SubmitResponseFactory submitResponseFactory;
    @Mock
    private RespondPossessionClaimSubmitService respondPossessionClaimSubmitService;
    @Mock
    private CounterClaimSubmitConfirmationService counterClaimSubmitConfirmationService;
    @Mock
    private EventPayload<PCSCase, State> eventPayload;

    private CitizenSubmissionEventStrategy underTest;

    @BeforeEach
    void setUp() {
        underTest = new CitizenSubmissionEventStrategy(
            draftCaseDataService,
            submitResponseFactory,
            respondPossessionClaimSubmitService,
            counterClaimSubmitConfirmationService
        );
    }

    @Test
    void shouldProcessDraft() {
        DefendantResponses responses = DefendantResponses.builder()
            .tenancyTypeConfirmation(YesNoNotSure.YES)
            .rentArrearsAmountConfirmation(YesNoNotSure.NO)
            .build();

        PCSCase caseData = createDraftSaveCaseData(responses);
        RespondPossessionClaimSubmitPersistenceResult persistenceResult =
            new RespondPossessionClaimSubmitPersistenceResult(caseData.getPossessionClaimResponse(), null, false);
        SubmitResponse<State> submitResponse = SubmitResponse.defaultResponse();

        stubDraft(caseData);
        when(eventPayload.caseReference()).thenReturn(CASE_REFERENCE);
        when(respondPossessionClaimSubmitService.persistFinalSubmit(
            CASE_REFERENCE,
            caseData.getPossessionClaimResponse()
        )).thenReturn(persistenceResult);
        when(counterClaimSubmitConfirmationService.buildSubmitResponse(CASE_REFERENCE, persistenceResult))
            .thenReturn(submitResponse);

        underTest.process(eventPayload);

        verify(submitResponseFactory).validate(caseData.getPossessionClaimResponse(), CASE_REFERENCE);
        verify(respondPossessionClaimSubmitService).persistFinalSubmit(
            CASE_REFERENCE,
            caseData.getPossessionClaimResponse()
        );
        verify(counterClaimSubmitConfirmationService).buildSubmitResponse(CASE_REFERENCE, persistenceResult);
    }

    @Test
    void shouldThrowExceptionWhenNoDraftFound() {
        DefendantResponses responses = DefendantResponses.builder()
            .tenancyTypeConfirmation(YesNoNotSure.YES)
            .rentArrearsAmountConfirmation(YesNoNotSure.NO)
            .build();

        PCSCase caseData = createDraftSaveCaseData(responses);

        when(draftCaseDataService.getUnsubmittedCaseData(CASE_REFERENCE, respondPossessionClaim))
            .thenReturn(Optional.empty());
        when(eventPayload.caseReference()).thenReturn(CASE_REFERENCE);

        assertThat(assertThrows(
            DraftNotFoundException.class,
            () -> underTest.process(eventPayload)
        )).hasMessage(String.format("No draft found for this case reference %s, eventId %s, and user ",
                                    CASE_REFERENCE, respondPossessionClaim));

        verify(submitResponseFactory, never()).validate(any(), anyLong());
        verify(respondPossessionClaimSubmitService, never()).persistFinalSubmit(anyLong(), any());
        verify(counterClaimSubmitConfirmationService, never()).buildSubmitResponse(anyLong(), any());
    }

    @Test
    void shouldReturnValidationErrors() {
        DefendantResponses responses = DefendantResponses.builder()
            .tenancyTypeConfirmation(YesNoNotSure.YES)
            .rentArrearsAmountConfirmation(YesNoNotSure.NO)
            .build();

        PossessionClaimResponse possessionClaimResponse = PossessionClaimResponse.builder()
            .defendantResponses(responses)
            .build();

        PCSCase caseData = PCSCase.builder()
            .possessionClaimResponse(possessionClaimResponse)
            .build();

        stubDraft(caseData);

        SubmitResponse<State> submitResponse = SubmitResponse.<State>builder()
            .errors(List.of("error"))
            .build();

        when(submitResponseFactory.validate(possessionClaimResponse, CASE_REFERENCE))
            .thenReturn(Optional.of(submitResponse));
        when(eventPayload.caseReference()).thenReturn(CASE_REFERENCE);

        underTest.process(eventPayload);

        verify(respondPossessionClaimSubmitService, never()).persistFinalSubmit(anyLong(), any());
        verify(counterClaimSubmitConfirmationService, never()).buildSubmitResponse(anyLong(), any());
    }

    private void stubDraft(PCSCase draft) {
        when(draftCaseDataService.getUnsubmittedCaseData(CASE_REFERENCE, respondPossessionClaim))
            .thenReturn(Optional.of(draft));
    }

    private PCSCase createDraftSaveCaseData(DefendantResponses responses) {
        PossessionClaimResponse response = PossessionClaimResponse.builder()
            .defendantResponses(responses != null ? responses : DefendantResponses.builder().build())
            .build();

        return PCSCase.builder()
            .possessionClaimResponse(response)
            .build();
    }

    @Test
    void supports_WithCitizenUser_ReturnsTrue() {
        assertThat(underTest.supports(List.of(UserRole.CITIZEN.getRole()))).isTrue();
    }

    @Test
    void supports_WithNonCitizenUser_ReturnsFalse() {
        assertThat(underTest.supports(List.of(UserRole.DEFENDANT_SOLICITOR.getRole()))).isFalse();
    }
}
