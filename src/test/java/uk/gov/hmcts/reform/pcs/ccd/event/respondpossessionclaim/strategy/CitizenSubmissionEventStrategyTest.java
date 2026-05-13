package uk.gov.hmcts.reform.pcs.ccd.event.respondpossessionclaim.strategy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.YesNoNotSure;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.DefendantContactDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.DefendantResponses;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.PossessionClaimResponse;
import uk.gov.hmcts.reform.pcs.ccd.service.DraftCaseDataService;
import uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim.ClaimResponseService;
import uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim.DefendantResponseService;
import uk.gov.hmcts.reform.pcs.exception.DraftNotFoundException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
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
    private ClaimResponseService claimResponseService;
    @Mock
    private DefendantResponseService defendantResponseService;
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
            .tenancyTypeConfirmation(YesNoNotSure.YES)
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
            .tenancyTypeConfirmation(YesNoNotSure.YES)
            .rentArrearsAmountConfirmation(YesNoNotSure.NO)
            .build();

        PCSCase caseData = createDraftSaveCaseData(null, responses);

        when(draftCaseDataService.getUnsubmittedCaseData(CASE_REFERENCE, respondPossessionClaim))
            .thenReturn(Optional.empty());

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
        verify(defendantResponseService, never()).saveDefendantResponse(CASE_REFERENCE,
                                                                        caseData.getPossessionClaimResponse());
        verify(draftCaseDataService, never()).deleteUnsubmittedCaseData(CASE_REFERENCE, respondPossessionClaim);
    }

    @Test
    void shouldReturnValidationErrors() {
        // given
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

        when(submitResponseFactory.validate(possessionClaimResponse, CASE_REFERENCE)).thenReturn(submitResponse);

        // when
        underTest.process(CASE_REFERENCE);

        // then
        verify(submitResponseFactory, never()).success();
        verify(claimResponseService, never()).saveDraftData(caseData.getPossessionClaimResponse(), CASE_REFERENCE);
        verify(defendantResponseService, never()).saveDefendantResponse(CASE_REFERENCE,
                                                                        caseData.getPossessionClaimResponse());
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
    void supports_WithCitizenUser_ReturnsTrue() {
        // when / then
        assertThat(underTest.supports(List.of(UserRole.CITIZEN.getRole()))).isTrue();
    }

    @Test
    void supports_WithNonCitizenUser_ReturnsFalse() {
        // when / then
        assertThat(underTest.supports(List.of(UserRole.DEFENDANT_SOLICITOR.getRole()))).isFalse();
    }

}
