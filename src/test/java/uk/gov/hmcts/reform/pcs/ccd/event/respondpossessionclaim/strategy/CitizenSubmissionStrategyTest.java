package uk.gov.hmcts.reform.pcs.ccd.event.respondpossessionclaim.strategy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.YesNoNotSure;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.DefendantContactDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.DefendantResponses;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.HouseholdCircumstances;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.PossessionClaimResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.RecurrenceFrequency;
import uk.gov.hmcts.reform.pcs.ccd.service.DraftCaseDataService;
import uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim.ClaimResponseService;
import uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim.DefendantResponseService;
import uk.gov.hmcts.reform.pcs.ccd.util.SelectedPartyRetriever;
import uk.gov.hmcts.reform.pcs.exception.DraftNotFoundException;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.util.List;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.respondPossessionClaim;

@ExtendWith(MockitoExtension.class)
class CitizenSubmissionStrategyTest {

    private static final long CASE_REFERENCE = 1234567890L;

    @Mock
    private DraftCaseDataService draftCaseDataService;
    @Mock
    private EventPayload<PCSCase, State> eventPayload;
    @Mock
    private PCSCase pcsCase;
    @Mock
    private ClaimResponseService claimResponseService;
    @Mock
    private DefendantResponseService defendantResponseService;
    @Mock
    private SelectedPartyRetriever selectedPartyRetriever;
    @Mock
    private SubmitResponseFactory submitResponseFactory;
    private CitizenSubmissionEventStrategy underTest;

    @BeforeEach
    void setUp() {
        underTest = new CitizenSubmissionEventStrategy(
            draftCaseDataService,
            claimResponseService,
            defendantResponseService,
            submitResponseFactory
        );
    }

    @Test
    void shouldProcessDraft() {
        // given
        DefendantResponses responses = DefendantResponses.builder()
            .tenancyTypeCorrect(YesNoNotSure.YES)
            .rentArrearsAmountConfirmation(YesNoNotSure.NO)
            .build();

        PCSCase caseData = createDraftSaveCaseData(null, responses);

        stubDraft(caseData);

        // when
        underTest.process(CASE_REFERENCE);

        // then
        verify(submitResponseFactory).success();
        verify(submitResponseFactory).validate(any(), anyLong());
        verify(claimResponseService).saveDraftData(caseData.getPossessionClaimResponse(), CASE_REFERENCE);
        verify(defendantResponseService).saveDefendantResponse(CASE_REFERENCE, caseData.getPossessionClaimResponse());
        verify(draftCaseDataService).deleteUnsubmittedCaseData(CASE_REFERENCE, respondPossessionClaim);
    }

    @Test
    void shouldThrowExceptionWhenNoDraftFound() {
        // given
        DefendantResponses responses = DefendantResponses.builder()
            .tenancyTypeCorrect(YesNoNotSure.YES)
            .rentArrearsAmountConfirmation(YesNoNotSure.NO)
            .build();

        PCSCase caseData = createDraftSaveCaseData(null, responses);

        when(draftCaseDataService.getUnsubmittedCaseData(CASE_REFERENCE, respondPossessionClaim)).thenReturn(Optional.empty());

        // when
        assertThat(assertThrows(
            DraftNotFoundException.class,
            () -> underTest.process(CASE_REFERENCE)
        )).hasMessage(String.format("No draft found for this case reference %s, eventId %s, and user ",
                                    CASE_REFERENCE, respondPossessionClaim));

        // then
        verify(submitResponseFactory, never()).success();
        verify(submitResponseFactory, never()).validate(any(), anyLong());
        verify(claimResponseService, never()).saveDraftData(caseData.getPossessionClaimResponse(), CASE_REFERENCE);
        verify(defendantResponseService, never()).saveDefendantResponse(CASE_REFERENCE, caseData.getPossessionClaimResponse());
        verify(draftCaseDataService, never()).deleteUnsubmittedCaseData(CASE_REFERENCE, respondPossessionClaim);
    }

    @Test
    void shouldReturnValidationErrors() {
        // given
        DefendantResponses responses = DefendantResponses.builder()
            .tenancyTypeCorrect(YesNoNotSure.YES)
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

        when(submitResponseFactory.validate(possessionClaimResponse, CASE_REFERENCE)).thenReturn(submitResponse);

        // when
        underTest.process(CASE_REFERENCE);

        // then
        verify(submitResponseFactory, never()).success();
        verify(claimResponseService, never()).saveDraftData(caseData.getPossessionClaimResponse(), CASE_REFERENCE);
        verify(defendantResponseService, never()).saveDefendantResponse(CASE_REFERENCE, caseData.getPossessionClaimResponse());
        verify(draftCaseDataService, never()).deleteUnsubmittedCaseData(CASE_REFERENCE, respondPossessionClaim);
    }

    private void stubDraft(PCSCase draft) {
        when(draftCaseDataService.getUnsubmittedCaseData(CASE_REFERENCE, respondPossessionClaim))
            .thenReturn(Optional.of(draft));
    }

    private PCSCase createDraftSaveCaseData(DefendantContactDetails contactDetails, DefendantResponses responses) {
        PossessionClaimResponse response = PossessionClaimResponse.builder()
            .defendantContactDetails(contactDetails)
            .defendantResponses(responses != null ? responses : DefendantResponses.builder().build())
            .build();

        return PCSCase.builder()
            .possessionClaimResponse(response)
            .build();
    }

    @Test
    void supports_ShouldReturnFalseWhenCitizen() {
        // when / then
        assertThat(underTest.supports(false)).isFalse();
    }

    @Test
    void supports_ShouldReturnTrueWhenNotCitizen() {
        // when / then
        assertThat(underTest.supports(true)).isTrue();
    }

}
