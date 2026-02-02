package uk.gov.hmcts.reform.pcs.ccd.event.respondpossessionclaim;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.ccd.domain.YesNoNotSure;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.DefendantContactDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.DefendantData;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.DefendantResponses;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.PossessionClaimResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.service.DraftCaseDataService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.respondPossessionClaim;

@ExtendWith(MockitoExtension.class)
class SubmitEventHandlerTest {

    private static final long CASE_REFERENCE = 1234567890L;

    @Mock
    private DraftCaseDataService draftCaseDataService;
    @Mock
    private EventPayload<PCSCase, State> eventPayload;

    @Captor
    private ArgumentCaptor<PCSCase> pcsCaseCaptor;

    private SubmitEventHandler underTest;

    @BeforeEach
    void setUp() {
        underTest = new SubmitEventHandler(draftCaseDataService);
    }

    // ========== DRAFT SAVE FLOW (submitDraftAnswers = NO) ==========

    @Test
    void shouldSaveDraftWhenSubmitFlagIsNo() {
        // Given
        AddressUK address = AddressUK.builder()
            .addressLine1("123 Test Street")
            .postTown("London")
            .postCode("SW1A 1AA")
            .build();

        Party party = Party.builder()
            .firstName("John")
            .lastName("Doe")
            .address(address)
            .build();

        DefendantContactDetails contactDetails = DefendantContactDetails.builder()
            .party(party)
            .build();

        DefendantData defendantData = DefendantData.builder()
            .contactDetails(contactDetails)
            .build();

        PossessionClaimResponse response = PossessionClaimResponse.builder()
            .defendantData(defendantData)
            .build();

        PCSCase caseData = PCSCase.builder()
            .possessionClaimResponse(response)
            .submitDraftAnswers(YesOrNo.NO)
            .build();

        EventPayload<PCSCase, State> eventPayload = createEventPayload(caseData);

        // When
        SubmitResponse<State> result = underTest.submit(eventPayload);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getErrors()).isNullOrEmpty();

        verify(draftCaseDataService).patchUnsubmittedEventData(
            eq(CASE_REFERENCE), pcsCaseCaptor.capture(), eq(respondPossessionClaim)
        );

        PCSCase savedDraft = pcsCaseCaptor.getValue();
        assertThat(savedDraft.getPossessionClaimResponse()).isNotNull();
        assertThat(savedDraft.getPossessionClaimResponse().getDefendantData()
            .getContactDetails().getParty().getFirstName()).isEqualTo("John");
        // Note: submitDraftAnswers is NOT persisted to draft - it's a transient UI flag
    }

    // ========== FINAL SUBMIT FLOW (submitDraftAnswers = YES) ==========

    @Test
    void shouldNotSaveDraftWhenSubmitFlagIsYes() {
        // Given
        Party party = Party.builder()
            .firstName("John")
            .lastName("Doe")
            .build();

        DefendantContactDetails contactDetails = DefendantContactDetails.builder()
            .party(party)
            .build();

        DefendantData defendantData = DefendantData.builder()
            .contactDetails(contactDetails)
            .build();

        PossessionClaimResponse response = PossessionClaimResponse.builder()
            .defendantData(defendantData)
            .build();

        PCSCase caseData = PCSCase.builder()
            .possessionClaimResponse(response)
            .submitDraftAnswers(YesOrNo.YES)
            .build();

        EventPayload<PCSCase, State> eventPayload = createEventPayload(caseData);

        // When
        SubmitResponse<State> result = underTest.submit(eventPayload);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getErrors()).isNullOrEmpty();

        verify(draftCaseDataService, never()).patchUnsubmittedEventData(
            eq(CASE_REFERENCE), any(PCSCase.class), eq(respondPossessionClaim)
        );
    }

    @Test
    void shouldProcessFinalSubmitWhenSubmitFlagIsYes() {
        // Given
        Party party = Party.builder()
            .firstName("John")
            .lastName("Doe")
            .build();

        DefendantContactDetails contactDetails = DefendantContactDetails.builder()
            .party(party)
            .build();

        DefendantResponses responses = DefendantResponses.builder()
            .tenancyTypeCorrect(YesNoNotSure.YES)
            .build();

        DefendantData defendantData = DefendantData.builder()
            .contactDetails(contactDetails)
            .responses(responses)
            .build();

        PossessionClaimResponse response = PossessionClaimResponse.builder()
            .defendantData(defendantData)
            .build();

        PCSCase caseData = PCSCase.builder()
            .possessionClaimResponse(response)
            .submitDraftAnswers(YesOrNo.YES)
            .build();

        EventPayload<PCSCase, State> eventPayload = createEventPayload(caseData);

        // When
        SubmitResponse<State> result = underTest.submit(eventPayload);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getErrors()).isNullOrEmpty();

        verify(draftCaseDataService, never()).patchUnsubmittedEventData(
            eq(CASE_REFERENCE), any(PCSCase.class), eq(respondPossessionClaim)
        );
    }

    // ========== VALIDATION ERROR CASES ==========

    @Test
    void shouldReturnErrorWhenPossessionClaimResponseIsNull() {
        // Given
        PCSCase caseData = PCSCase.builder()
            .possessionClaimResponse(null)
            .submitDraftAnswers(YesOrNo.NO)
            .build();

        EventPayload<PCSCase, State> eventPayload = createEventPayload(caseData);

        // When
        SubmitResponse<State> result = underTest.submit(eventPayload);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getErrors()).isNotNull();
        assertThat(result.getErrors()).hasSize(1);
        assertThat(result.getErrors().get(0)).isEqualTo("Invalid submission: missing response data");

        verify(draftCaseDataService, never()).patchUnsubmittedEventData(
            eq(CASE_REFERENCE), any(PCSCase.class), eq(respondPossessionClaim)
        );
    }

    @Test
    void shouldDefaultToNoAndSaveDraftWhenSubmitFlagIsNull() {
        // Given
        Party party = Party.builder()
            .firstName("John")
            .build();

        DefendantContactDetails contactDetails = DefendantContactDetails.builder()
            .party(party)
            .build();

        DefendantData defendantData = DefendantData.builder()
            .contactDetails(contactDetails)
            .build();

        PossessionClaimResponse response = PossessionClaimResponse.builder()
            .defendantData(defendantData)
            .build();

        PCSCase caseData = PCSCase.builder()
            .possessionClaimResponse(response)
            .submitDraftAnswers(null)
            .build();

        EventPayload<PCSCase, State> eventPayload = createEventPayload(caseData);

        // When
        SubmitResponse<State> result = underTest.submit(eventPayload);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getErrors()).isNullOrEmpty();

        verify(draftCaseDataService).patchUnsubmittedEventData(
            eq(CASE_REFERENCE), any(PCSCase.class), eq(respondPossessionClaim)
        );
    }

    @Test
    void shouldReturnErrorWhenDefendantDataIsNull() {
        // Given
        PossessionClaimResponse response = PossessionClaimResponse.builder()
            .defendantData(null)
            .build();

        PCSCase caseData = PCSCase.builder()
            .possessionClaimResponse(response)
            .submitDraftAnswers(YesOrNo.NO)
            .build();

        EventPayload<PCSCase, State> eventPayload = createEventPayload(caseData);

        // When
        SubmitResponse<State> result = underTest.submit(eventPayload);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getErrors()).isNotNull();
        assertThat(result.getErrors()).hasSize(1);
        assertThat(result.getErrors().get(0))
            .isEqualTo("Invalid response structure. Please refresh the page and try again.");

        verify(draftCaseDataService, never()).patchUnsubmittedEventData(
            eq(CASE_REFERENCE), any(PCSCase.class), eq(respondPossessionClaim)
        );
    }

    /**
     * REGRESSION GUARD: These tests will FAIL if validation is made stricter.
     * DO NOT change validation to require contactDetails != null or party != null.
     * Partial updates are required for incremental form saves.
     */

    @Test
    void shouldAllowPartialUpdateWhenOnlyResponsesProvided() {
        // Given
        DefendantResponses responses = DefendantResponses.builder()
            .tenancyTypeCorrect(YesNoNotSure.NO)
            .oweRentArrears(YesNoNotSure.YES)
            .build();

        DefendantData defendantData = DefendantData.builder()
            .responses(responses)
            .contactDetails(null)
            .build();

        // When
        PCSCase caseData = createDraftSaveCaseData(defendantData);

        // Then
        submitAndVerifyDraftSaved(caseData);
    }

    @Test
    void shouldAllowPartialUpdateWhenOnlyContactDetailsProvided() {
        // Given
        Party party = Party.builder()
            .firstName("Jane")
            .lastName("Smith")
            .build();

        DefendantContactDetails contactDetails = DefendantContactDetails.builder()
            .party(party)
            .build();

        DefendantData defendantData = DefendantData.builder()
            .contactDetails(contactDetails)
            .responses(null)
            .build();

        // When
        PCSCase caseData = createDraftSaveCaseData(defendantData);

        // Then
        submitAndVerifyDraftSaved(caseData);
    }

    @Test
    void shouldAllowPartialUpdateWhenPartyIsNull() {
        // Given
        DefendantContactDetails contactDetails = DefendantContactDetails.builder()
            .party(null)
            .build();

        DefendantData defendantData = DefendantData.builder()
            .contactDetails(contactDetails)
            .build();

        // When
        PCSCase caseData = createDraftSaveCaseData(defendantData);

        // Then
        submitAndVerifyDraftSaved(caseData);
    }

    @Test
    void shouldAllowPartialUpdateWhenOnlySingleResponseFieldProvided() {
        // Given
        DefendantResponses responses = DefendantResponses.builder()
            .tenancyTypeCorrect(YesNoNotSure.NO)
            .build();

        DefendantData defendantData = DefendantData.builder()
            .responses(responses)
            .contactDetails(null)
            .build();

        // When
        PCSCase caseData = createDraftSaveCaseData(defendantData);

        // Then
        submitAndVerifyDraftSaved(caseData);
    }

    @Test
    void shouldAllowPartialUpdateWhenOnlyAddressLine1Provided() {
        // Given
        AddressUK address = AddressUK.builder()
            .addressLine1("25 New Street")
            .build();

        Party party = Party.builder()
            .address(address)
            .build();

        DefendantContactDetails contactDetails = DefendantContactDetails.builder()
            .party(party)
            .build();

        DefendantData defendantData = DefendantData.builder()
            .contactDetails(contactDetails)
            .responses(null)
            .build();

        // When
        PCSCase caseData = createDraftSaveCaseData(defendantData);

        // Then
        submitAndVerifyDraftSaved(caseData);
    }

    @Test
    void shouldAllowPartialUpdateWhenOnlyFirstNameProvided() {
        // Given
        Party party = Party.builder()
            .firstName("Jane")
            .build();

        DefendantContactDetails contactDetails = DefendantContactDetails.builder()
            .party(party)
            .build();

        DefendantData defendantData = DefendantData.builder()
            .contactDetails(contactDetails)
            .responses(null)
            .build();

        // When
        PCSCase caseData = createDraftSaveCaseData(defendantData);

        // Then
        submitAndVerifyDraftSaved(caseData);
    }

    @Test
    void shouldReturnErrorWhenDraftServiceThrowsException() {
        // Given
        Party party = Party.builder()
            .firstName("John")
            .lastName("Doe")
            .build();

        DefendantContactDetails contactDetails = DefendantContactDetails.builder()
            .party(party)
            .build();

        DefendantData defendantData = DefendantData.builder()
            .contactDetails(contactDetails)
            .build();

        PossessionClaimResponse response = PossessionClaimResponse.builder()
            .defendantData(defendantData)
            .build();

        PCSCase caseData = PCSCase.builder()
            .possessionClaimResponse(response)
            .submitDraftAnswers(YesOrNo.NO)
            .build();

        EventPayload<PCSCase, State> eventPayload = createEventPayload(caseData);

        doThrow(new RuntimeException("Database connection failed"))
            .when(draftCaseDataService).patchUnsubmittedEventData(
                eq(CASE_REFERENCE), any(PCSCase.class), eq(respondPossessionClaim)
            );

        // When
        SubmitResponse<State> result = underTest.submit(eventPayload);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getErrors()).isNotNull();
        assertThat(result.getErrors()).hasSize(1);
        assertThat(result.getErrors().get(0))
            .isEqualTo("We couldn't save your response. Please try again or contact support.");
    }

    @Test
    void shouldReturnDefaultResponseWhenDraftSavedSuccessfully() {
        // Given
        AddressUK address = AddressUK.builder()
            .addressLine1("123 Test Street")
            .postTown("London")
            .postCode("SW1A 1AA")
            .build();

        Party party = Party.builder()
            .firstName("John")
            .lastName("Doe")
            .address(address)
            .build();

        DefendantContactDetails contactDetails = DefendantContactDetails.builder()
            .party(party)
            .build();

        DefendantData defendantData = DefendantData.builder()
            .contactDetails(contactDetails)
            .build();

        PossessionClaimResponse response = PossessionClaimResponse.builder()
            .defendantData(defendantData)
            .build();

        PCSCase caseData = PCSCase.builder()
            .possessionClaimResponse(response)
            .submitDraftAnswers(YesOrNo.NO)
            .build();

        EventPayload<PCSCase, State> eventPayload = createEventPayload(caseData);

        // When
        SubmitResponse<State> result = underTest.submit(eventPayload);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getErrors()).isNullOrEmpty();

        verify(draftCaseDataService).patchUnsubmittedEventData(
            eq(CASE_REFERENCE), any(PCSCase.class), eq(respondPossessionClaim)
        );
    }

    @Test
    void shouldPassCorrectDataToDraftServiceWhenSaving() {
        // Given
        AddressUK address = AddressUK.builder()
            .addressLine1("123 Test Street")
            .postTown("London")
            .postCode("SW1A 1AA")
            .build();

        Party party = Party.builder()
            .firstName("John")
            .lastName("Doe")
            .emailAddress("john@example.com")
            .phoneNumber("07700900000")
            .address(address)
            .build();

        DefendantContactDetails contactDetails = DefendantContactDetails.builder()
            .party(party)
            .build();

        DefendantData defendantData = DefendantData.builder()
            .contactDetails(contactDetails)
            .build();

        PossessionClaimResponse response = PossessionClaimResponse.builder()
            .defendantData(defendantData)
            .build();

        PCSCase caseData = PCSCase.builder()
            .possessionClaimResponse(response)
            .submitDraftAnswers(YesOrNo.NO)
            .build();

        EventPayload<PCSCase, State> eventPayload = createEventPayload(caseData);

        // When
        underTest.submit(eventPayload);

        // Then
        verify(draftCaseDataService).patchUnsubmittedEventData(
            eq(CASE_REFERENCE), pcsCaseCaptor.capture(), eq(respondPossessionClaim)
        );

        PCSCase savedDraft = pcsCaseCaptor.getValue();
        assertThat(savedDraft.getPossessionClaimResponse()).isNotNull();
        DefendantData savedDefendantData = savedDraft.getPossessionClaimResponse().getDefendantData();
        assertThat(savedDefendantData).isNotNull();
        assertThat(savedDefendantData.getContactDetails()).isNotNull();
        assertThat(savedDefendantData.getContactDetails().getParty()).isNotNull();
        assertThat(savedDefendantData.getContactDetails().getParty().getFirstName()).isEqualTo("John");
        assertThat(savedDefendantData.getContactDetails().getParty().getLastName()).isEqualTo("Doe");
        assertThat(savedDefendantData.getContactDetails().getParty().getEmailAddress())
            .isEqualTo("john@example.com");
        assertThat(savedDefendantData.getContactDetails().getParty().getPhoneNumber())
            .isEqualTo("07700900000");
        assertThat(savedDefendantData.getContactDetails().getParty().getAddress()).isEqualTo(address);
        // Note: submitDraftAnswers is NOT persisted to draft - it's a transient UI flag
    }

    private EventPayload<PCSCase, State> createEventPayload(PCSCase caseData) {
        when(eventPayload.caseReference()).thenReturn(CASE_REFERENCE);
        when(eventPayload.caseData()).thenReturn(caseData);
        return eventPayload;
    }

    private PCSCase createDraftSaveCaseData(DefendantData defendantData) {
        PossessionClaimResponse response = PossessionClaimResponse.builder()
            .defendantData(defendantData)
            .build();

        return PCSCase.builder()
            .possessionClaimResponse(response)
            .submitDraftAnswers(YesOrNo.NO)
            .build();
    }

    private void submitAndVerifyDraftSaved(PCSCase caseData) {
        EventPayload<PCSCase, State> eventPayload = createEventPayload(caseData);
        SubmitResponse<State> result = underTest.submit(eventPayload);

        assertThat(result).isNotNull();
        assertThat(result.getErrors()).isNullOrEmpty();

        verify(draftCaseDataService).patchUnsubmittedEventData(
            eq(CASE_REFERENCE), any(PCSCase.class), eq(respondPossessionClaim)
        );
    }
}
