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
import uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim.ClaimResponseService;
import uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim.DefendantResponseService;
import uk.gov.hmcts.reform.pcs.ccd.util.SelectedPartyRetriever;
import uk.gov.hmcts.reform.pcs.exception.DraftNotFoundException;
import uk.gov.hmcts.reform.pcs.reference.service.OrganisationDetailsService;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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
    private ClaimResponseService claimResponseService;
    @Mock
    private DefendantResponseService defendantResponseService;
    @Mock
    private SelectedPartyRetriever selectedPartyRetriever;
    @Mock
    private SubmitResponseFactory submitResponseFactory;
    @Mock
    private EventPayload<PCSCase, State> eventPayload;
    private LegalRepSubmissionEventStrategy underTest;
    @Mock
    private OrganisationDetailsService organisationDetailsService;
    @Mock
    private SecurityContextService securityContextService;


    @BeforeEach
    void setUp() {
        underTest = new LegalRepSubmissionEventStrategy(
            draftCaseDataService,
            claimResponseService,
            defendantResponseService,
            selectedPartyRetriever,
            submitResponseFactory,
            organisationDetailsService,
            securityContextService
        );
    }

    @Test
    void shouldSubmitLegalRepresentativeDraftForSelectedParty() {
        // given
        UUID representedPartyId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        String organisationId = "org";
        UUID legalRepOrgId = UUID.randomUUID();

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

        when(selectedPartyRetriever.getCurrentRepresentedPartyId(caseData)).thenReturn(Optional.of(representedPartyId));
        when(draftCaseDataService.getUnsubmittedCaseData(CASE_REFERENCE, respondPossessionClaim, representedPartyId,
                                                         organisationId))
            .thenReturn(Optional.of(caseData));
        when(submitResponseFactory.success()).thenReturn(submitResponse);
        when(eventPayload.caseReference()).thenReturn(CASE_REFERENCE);
        when(eventPayload.caseData()).thenReturn(caseData);
        when(securityContextService.getCurrentUserId()).thenReturn(userId);
        when(organisationDetailsService.getOrganisationIdentifier(userId.toString())).thenReturn(organisationId);

        // when
        SubmitResponse<State> result = underTest.process(eventPayload);

        // then
        assertThat(result.getErrors()).isNullOrEmpty();
        verify(claimResponseService).saveDraftDataForParty(possessionClaimResponse, CASE_REFERENCE, representedPartyId);
        verify(defendantResponseService).saveDefendantResponse(CASE_REFERENCE, possessionClaimResponse,
                                                               representedPartyId);
        verify(draftCaseDataService).deleteUnsubmittedCaseData(CASE_REFERENCE, respondPossessionClaim,
                                                               representedPartyId, organisationId);
        verify(draftCaseDataService, never()).getUnsubmittedCaseData(CASE_REFERENCE, respondPossessionClaim);
    }

    @Test
    void shouldThrowExceptionForNoSelectedParty() {
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

        when(selectedPartyRetriever.getCurrentRepresentedPartyId(caseData)).thenReturn(Optional.empty());
        when(eventPayload.caseReference()).thenReturn(CASE_REFERENCE);
        when(eventPayload.caseData()).thenReturn(caseData);

        // when / then
        assertThat(assertThrows(
            IllegalStateException.class,
            () -> underTest.process(eventPayload)
        )).hasMessage("No selected responding party id for respond to claim");

        verify(claimResponseService, never())
            .saveDraftDataForParty(possessionClaimResponse, CASE_REFERENCE, representedPartyId);
        verify(defendantResponseService, never()).saveDefendantResponse(CASE_REFERENCE, possessionClaimResponse,
                                                                        representedPartyId);
        verify(draftCaseDataService, never()).deleteUnsubmittedCaseData(eq(CASE_REFERENCE), eq(respondPossessionClaim),
                                                                        eq(representedPartyId), anyString());
        verify(draftCaseDataService, never()).getUnsubmittedCaseData(CASE_REFERENCE, respondPossessionClaim);
    }

    @Test
    void shouldReturnValidationErrors() {
        // given
        UUID representedPartyId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        String organisationId = "org";
        UUID legalRepOrgId = UUID.randomUUID();

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

        when(selectedPartyRetriever.getCurrentRepresentedPartyId(caseData)).thenReturn(Optional.of(representedPartyId));
        when(draftCaseDataService.getUnsubmittedCaseData(CASE_REFERENCE, respondPossessionClaim, representedPartyId,
                                                         organisationId))
            .thenReturn(Optional.of(caseData));
        when(submitResponseFactory.validate(possessionClaimResponse, CASE_REFERENCE))
            .thenReturn(Optional.of(submitResponse));
        when(eventPayload.caseReference()).thenReturn(CASE_REFERENCE);
        when(eventPayload.caseData()).thenReturn(caseData);
        when(securityContextService.getCurrentUserId()).thenReturn(userId);
        when(organisationDetailsService.getOrganisationIdentifier(userId.toString())).thenReturn(organisationId);

        // when
        SubmitResponse<State> result = underTest.process(eventPayload);

        // then
        assertThat(result.getErrors()).contains("error");
        verify(claimResponseService, never()).saveDraftDataForParty(possessionClaimResponse, CASE_REFERENCE,
                                                                    representedPartyId);
        verify(defendantResponseService, never()).saveDefendantResponse(CASE_REFERENCE, possessionClaimResponse,
                                                                        representedPartyId);
        verify(draftCaseDataService, never()).deleteUnsubmittedCaseData(eq(CASE_REFERENCE), eq(respondPossessionClaim),
                                                                        eq(representedPartyId), anyString());
        verify(draftCaseDataService, never()).getUnsubmittedCaseData(CASE_REFERENCE, respondPossessionClaim);
    }

    @Test
    void shouldThrowExceptionWhenNoDraft() {
        // Given
        UUID representedPartyId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        String organisationId = "org";
        UUID legalRepOrgId = UUID.randomUUID();
        PCSCase caseData = PCSCase.builder()
            .build();
        when(selectedPartyRetriever.getCurrentRepresentedPartyId(caseData)).thenReturn(Optional.of(representedPartyId));
        when(eventPayload.caseReference()).thenReturn(CASE_REFERENCE);
        when(eventPayload.caseData()).thenReturn(caseData);
        when(securityContextService.getCurrentUserId()).thenReturn(userId);
        when(organisationDetailsService.getOrganisationIdentifier(userId.toString())).thenReturn(organisationId);

        // When
        assertThatThrownBy(() -> underTest.process(eventPayload))
            .isInstanceOf(DraftNotFoundException.class);

        // Then
        verify(claimResponseService, never()).saveDraftDataForParty(any(), anyLong(), any());
        verify(defendantResponseService, never()).saveDefendantResponse(anyLong(), any(), any());
        verify(draftCaseDataService, never()).deleteUnsubmittedCaseData(anyLong(), any(), any(), any());
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
