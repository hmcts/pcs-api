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
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.DefendantResponses;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.PossessionClaimResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.event.EventId;
import uk.gov.hmcts.reform.pcs.ccd.service.PossessionClaimResponsePersistenceService;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.service.DraftCaseDataService;
import uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim.ImmutablePartyFieldValidator;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.respondPossessionClaim;

@ExtendWith(MockitoExtension.class)
class SubmitEventHandlerTest {

    private static final long CASE_REFERENCE = 1234567890L;

    @Mock
    private DraftCaseDataService draftCaseDataService;
    @Mock
    private ImmutablePartyFieldValidator immutableFieldValidator;
    @Mock
    private EventPayload<PCSCase, State> eventPayload;
    @Mock
    PossessionClaimResponsePersistenceService possessionClaimResponsePersistenceService;

    @Captor
    private ArgumentCaptor<PCSCase> pcsCaseCaptor;

    private SubmitEventHandler underTest;

    @BeforeEach
    void setUp() {
        underTest =
            new SubmitEventHandler(draftCaseDataService,
                immutableFieldValidator, possessionClaimResponsePersistenceService);
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

        PossessionClaimResponse response = PossessionClaimResponse.builder()
            .defendantContactDetails(contactDetails)
            .defendantResponses(DefendantResponses.builder().build())
            .build();

        PCSCase caseData = PCSCase.builder()
            .possessionClaimResponse(response)
            .submitDraftAnswers(YesOrNo.NO)
            .build();

        EventPayload<PCSCase, State> payload = createEventPayload(caseData);

        // When
        SubmitResponse<State> result = underTest.submit(payload);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getErrors()).isNullOrEmpty();

        verify(draftCaseDataService).patchUnsubmittedEventData(
            eq(CASE_REFERENCE), pcsCaseCaptor.capture(), eq(respondPossessionClaim)
        );

        PCSCase savedDraft = pcsCaseCaptor.getValue();
        assertThat(savedDraft.getPossessionClaimResponse()).isNotNull();
        assertThat(savedDraft.getPossessionClaimResponse().getDefendantContactDetails()
            .getParty().getFirstName()).isEqualTo("John");
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

        PossessionClaimResponse response = PossessionClaimResponse.builder()
            .defendantContactDetails(contactDetails)
            .defendantResponses(DefendantResponses.builder().build())
            .build();

        PCSCase caseData = PCSCase.builder()
            .possessionClaimResponse(response)
            .submitDraftAnswers(YesOrNo.YES)
            .build();

        EventPayload<PCSCase, State> payload = createEventPayload(caseData);

        // When
        when(draftCaseDataService.getUnsubmittedCaseData(CASE_REFERENCE, respondPossessionClaim))
            .thenReturn(Optional.of(caseData));
        SubmitResponse<State> result = underTest.submit(payload);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getErrors()).isNullOrEmpty();

        verify(draftCaseDataService, times(1)).patchUnsubmittedEventData(
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

        PossessionClaimResponse response = PossessionClaimResponse.builder()
            .defendantContactDetails(contactDetails)
            .defendantResponses(responses)
            .build();

        PCSCase caseData = PCSCase.builder()
            .possessionClaimResponse(response)
            .submitDraftAnswers(YesOrNo.YES)
            .build();

        EventPayload<PCSCase, State> payload = createEventPayload(caseData);

        // When
        when(draftCaseDataService.getUnsubmittedCaseData(CASE_REFERENCE, respondPossessionClaim))
            .thenReturn(Optional.of(caseData));

        SubmitResponse<State> result = underTest.submit(payload);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getErrors()).isNullOrEmpty();

        verify(draftCaseDataService, times(1)).patchUnsubmittedEventData(
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

        EventPayload<PCSCase, State> payload = createEventPayload(caseData);

        // When
        SubmitResponse<State> result = underTest.submit(payload);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getErrors()).isNotNull();
        assertThat(result.getErrors()).hasSize(1);
        assertThat(result.getErrors().getFirst()).isEqualTo("Invalid submission: missing response data");

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

        PossessionClaimResponse response = PossessionClaimResponse.builder()
            .defendantContactDetails(contactDetails)
            .defendantResponses(DefendantResponses.builder().build())
            .build();

        PCSCase caseData = PCSCase.builder()
            .possessionClaimResponse(response)
            .submitDraftAnswers(null)
            .build();

        EventPayload<PCSCase, State> payload = createEventPayload(caseData);

        // When
        SubmitResponse<State> result = underTest.submit(payload);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getErrors()).isNullOrEmpty();

        verify(draftCaseDataService).patchUnsubmittedEventData(
            eq(CASE_REFERENCE), any(PCSCase.class), eq(respondPossessionClaim)
        );
    }

    @Test
    void shouldReturnErrorWhenBothContactDetailsAndResponsesAreNull() {
        // Given - Both defendantContactDetails and defendantResponses are null
        PossessionClaimResponse response = PossessionClaimResponse.builder()
            .defendantContactDetails(null)
            .defendantResponses(null)
            .build();

        PCSCase caseData = PCSCase.builder()
            .possessionClaimResponse(response)
            .submitDraftAnswers(YesOrNo.NO)
            .build();

        EventPayload<PCSCase, State> payload = createEventPayload(caseData);

        // When
        SubmitResponse<State> result = underTest.submit(payload);

        // Then - Must reject when both fields are null
        assertThat(result).isNotNull();
        assertThat(result.getErrors())
            .as("Must return error when both fields are null")
            .isNotNull()
            .hasSize(1);
        assertThat(result.getErrors().getFirst())
            .as("Error message should indicate no data to save")
            .isEqualTo("Invalid submission: no data to save");

        verify(draftCaseDataService, never()).patchUnsubmittedEventData(
            eq(CASE_REFERENCE), any(PCSCase.class), eq(respondPossessionClaim)
        );
    }

    @Test
    void shouldRejectDraftWhenImmutableFieldNameKnownIsSent() {
        // Given - Party with immutable field nameKnown
        Party party = Party.builder()
            .firstName("John")
            .lastName("Doe")
            .nameKnown(VerticalYesNo.YES)  // Immutable field with value
            .build();

        DefendantContactDetails contactDetails = DefendantContactDetails.builder()
            .party(party)
            .build();

        PossessionClaimResponse response = PossessionClaimResponse.builder()
            .defendantContactDetails(contactDetails)
            .build();

        PCSCase caseData = PCSCase.builder()
            .possessionClaimResponse(response)
            .submitDraftAnswers(YesOrNo.NO)
            .build();

        EventPayload<PCSCase, State> payload = createEventPayload(caseData);

        when(immutableFieldValidator.findImmutableFieldViolations(party, CASE_REFERENCE))
            .thenReturn(List.of("nameKnown"));

        // When
        SubmitResponse<State> result = underTest.submit(payload);

        // Then
        assertThat(result.getErrors())
            .as("Must reject when immutable field is sent")
            .hasSize(1)
            .contains("Invalid submission: immutable field must not be sent: nameKnown");

        verify(draftCaseDataService, never()).patchUnsubmittedEventData(
            eq(CASE_REFERENCE), any(PCSCase.class), eq(respondPossessionClaim)
        );
    }

    @Test
    void shouldRejectDraftWhenMultipleImmutableFieldsSent() {
        // Given - Party with multiple immutable fields
        Party party = Party.builder()
            .firstName("John")
            .lastName("Doe")
            .nameKnown(VerticalYesNo.YES)
            .addressKnown(VerticalYesNo.YES)
            .addressSameAsProperty(VerticalYesNo.NO)
            .build();

        DefendantContactDetails contactDetails = DefendantContactDetails.builder()
            .party(party)
            .build();

        PossessionClaimResponse response = PossessionClaimResponse.builder()
            .defendantContactDetails(contactDetails)
            .build();

        PCSCase caseData = PCSCase.builder()
            .possessionClaimResponse(response)
            .submitDraftAnswers(YesOrNo.NO)
            .build();

        EventPayload<PCSCase, State> payload = createEventPayload(caseData);

        when(immutableFieldValidator.findImmutableFieldViolations(party, CASE_REFERENCE))
            .thenReturn(List.of("nameKnown", "addressKnown", "addressSameAsProperty"));

        // When
        SubmitResponse<State> result = underTest.submit(payload);

        // Then
        assertThat(result.getErrors())
            .as("Must reject when multiple immutable fields sent")
            .hasSize(3)
            .containsExactlyInAnyOrder(
                "Invalid submission: immutable field must not be sent: nameKnown",
                "Invalid submission: immutable field must not be sent: addressKnown",
                "Invalid submission: immutable field must not be sent: addressSameAsProperty"
            );

        verify(draftCaseDataService, never()).patchUnsubmittedEventData(
            eq(CASE_REFERENCE), any(PCSCase.class), eq(respondPossessionClaim)
        );
    }

    @Test
    void shouldAcceptDraftWhenOnlyEditableFieldsSent() {
        // Given - Party with ONLY editable fields (no immutable fields)
        Party party = Party.builder()
            .firstName("John")
            .lastName("Doe")
            .emailAddress("john@example.com")
            .phoneNumber("07700900000")
            .build();

        DefendantContactDetails contactDetails = DefendantContactDetails.builder()
            .party(party)
            .build();

        PossessionClaimResponse response = PossessionClaimResponse.builder()
            .defendantContactDetails(contactDetails)
            .build();

        PCSCase caseData = PCSCase.builder()
            .possessionClaimResponse(response)
            .submitDraftAnswers(YesOrNo.NO)
            .build();

        EventPayload<PCSCase, State> payload = createEventPayload(caseData);

        when(immutableFieldValidator.findImmutableFieldViolations(party, CASE_REFERENCE))
            .thenReturn(List.of());  // No violations

        // When
        SubmitResponse<State> result = underTest.submit(payload);

        // Then
        assertThat(result.getErrors())
            .as("Should accept when no immutable fields sent")
            .isNullOrEmpty();

        verify(draftCaseDataService).patchUnsubmittedEventData(
            eq(CASE_REFERENCE), any(PCSCase.class), eq(respondPossessionClaim)
        );
    }

    // ========== INDEPENDENT FIELD SUBMISSION TESTS ==========

    @Test
    void shouldAllowSubmitWithOnlyDefendantResponses() {
        // Given - Only defendantResponses, defendantContactDetails is null
        DefendantResponses responses = DefendantResponses.builder()
            .tenancyTypeCorrect(YesNoNotSure.YES)
            .oweRentArrears(YesNoNotSure.NO)
            .build();

        PossessionClaimResponse response = PossessionClaimResponse.builder()
            .defendantContactDetails(null)
            .defendantResponses(responses)
            .build();

        PCSCase caseData = PCSCase.builder()
            .possessionClaimResponse(response)
            .submitDraftAnswers(YesOrNo.NO)
            .build();

        EventPayload<PCSCase, State> payload = createEventPayload(caseData);

        // When
        SubmitResponse<State> result = underTest.submit(payload);

        // Then - Must succeed when only responses provided
        assertThat(result)
            .as("Submit must succeed when only defendantResponses provided")
            .isNotNull();
        assertThat(result.getErrors())
            .as("No errors when defendantContactDetails=null and defendantResponses!=null")
            .isNullOrEmpty();

        verify(draftCaseDataService).patchUnsubmittedEventData(
            eq(CASE_REFERENCE), pcsCaseCaptor.capture(), eq(respondPossessionClaim)
        );

        PCSCase savedDraft = pcsCaseCaptor.getValue();
        assertThat(savedDraft.getPossessionClaimResponse()).isNotNull();
        assertThat(savedDraft.getPossessionClaimResponse().getDefendantContactDetails())
            .as("defendantContactDetails should remain null when not provided")
            .isNull();
        assertThat(savedDraft.getPossessionClaimResponse().getDefendantResponses())
            .as("defendantResponses should be saved")
            .isNotNull();
        assertThat(savedDraft.getPossessionClaimResponse().getDefendantResponses()
            .getTenancyTypeCorrect()).isEqualTo(YesNoNotSure.YES);
    }

    @Test
    void shouldAllowSubmitWithOnlyDefendantContactDetails() {
        // Given - Only defendantContactDetails, defendantResponses is null
        Party party = Party.builder()
            .firstName("Jane")
            .lastName("Doe")
            .emailAddress("jane@example.com")
            .build();

        DefendantContactDetails contactDetails = DefendantContactDetails.builder()
            .party(party)
            .build();

        PossessionClaimResponse response = PossessionClaimResponse.builder()
            .defendantContactDetails(contactDetails)
            .defendantResponses(null)
            .build();

        PCSCase caseData = PCSCase.builder()
            .possessionClaimResponse(response)
            .submitDraftAnswers(YesOrNo.NO)
            .build();

        EventPayload<PCSCase, State> payload = createEventPayload(caseData);

        // When
        SubmitResponse<State> result = underTest.submit(payload);

        // Then - Must succeed when only contact details provided
        assertThat(result)
            .as("Submit must succeed when only defendantContactDetails provided")
            .isNotNull();
        assertThat(result.getErrors())
            .as("No errors when defendantResponses=null and defendantContactDetails!=null")
            .isNullOrEmpty();

        verify(draftCaseDataService).patchUnsubmittedEventData(
            eq(CASE_REFERENCE), pcsCaseCaptor.capture(), eq(respondPossessionClaim)
        );

        PCSCase savedDraft = pcsCaseCaptor.getValue();
        assertThat(savedDraft.getPossessionClaimResponse()).isNotNull();
        assertThat(savedDraft.getPossessionClaimResponse().getDefendantContactDetails())
            .as("defendantContactDetails should be saved")
            .isNotNull();
        assertThat(savedDraft.getPossessionClaimResponse().getDefendantContactDetails()
            .getParty().getFirstName()).isEqualTo("Jane");
        assertThat(savedDraft.getPossessionClaimResponse().getDefendantResponses())
            .as("defendantResponses should remain null when not provided")
            .isNull();
    }

    // ========== PARTIAL FIELD UPDATE TESTS ==========
    // REGRESSION GUARD: Validation must allow partial party fields (e.g., only firstName, only address).

    @Test
    void shouldAllowPartialUpdateWhenOnlyResponsesProvided() {
        // Given
        DefendantResponses responses = DefendantResponses.builder()
            .tenancyTypeCorrect(YesNoNotSure.NO)
            .oweRentArrears(YesNoNotSure.YES)
            .build();

        DefendantContactDetails contactDetails = DefendantContactDetails.builder()
            .party(null)
            .build();

        // When
        PCSCase caseData = createDraftSaveCaseData(contactDetails, responses);

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

        // When
        PCSCase caseData = createDraftSaveCaseData(contactDetails, null);

        // Then
        submitAndVerifyDraftSaved(caseData);
    }

    @Test
    void shouldAllowPartialUpdateWhenPartyIsNull() {
        // Given
        DefendantContactDetails contactDetails = DefendantContactDetails.builder()
            .party(null)
            .build();

        // When
        PCSCase caseData = createDraftSaveCaseData(contactDetails, null);

        // Then
        submitAndVerifyDraftSaved(caseData);
    }

    @Test
    void shouldAllowPartialUpdateWhenOnlySingleResponseFieldProvided() {
        // Given
        DefendantResponses responses = DefendantResponses.builder()
            .tenancyTypeCorrect(YesNoNotSure.NO)
            .build();

        DefendantContactDetails contactDetails = DefendantContactDetails.builder()
            .party(null)
            .build();

        // When
        PCSCase caseData = createDraftSaveCaseData(contactDetails, responses);

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

        // When
        PCSCase caseData = createDraftSaveCaseData(contactDetails, null);

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

        // When
        PCSCase caseData = createDraftSaveCaseData(contactDetails, null);

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

        PossessionClaimResponse response = PossessionClaimResponse.builder()
            .defendantContactDetails(contactDetails)
            .defendantResponses(DefendantResponses.builder().build())
            .build();

        PCSCase caseData = PCSCase.builder()
            .possessionClaimResponse(response)
            .submitDraftAnswers(YesOrNo.NO)
            .build();

        EventPayload<PCSCase, State> payload = createEventPayload(caseData);

        doThrow(new RuntimeException("Database connection failed"))
            .when(draftCaseDataService).patchUnsubmittedEventData(
                eq(CASE_REFERENCE), any(PCSCase.class), eq(respondPossessionClaim)
            );

        // When
        SubmitResponse<State> result = underTest.submit(payload);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getErrors()).isNotNull();
        assertThat(result.getErrors()).hasSize(1);
        assertThat(result.getErrors().getFirst())
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

        PossessionClaimResponse response = PossessionClaimResponse.builder()
            .defendantContactDetails(contactDetails)
            .defendantResponses(DefendantResponses.builder().build())
            .build();

        PCSCase caseData = PCSCase.builder()
            .possessionClaimResponse(response)
            .submitDraftAnswers(YesOrNo.NO)
            .build();

        EventPayload<PCSCase, State> payload = createEventPayload(caseData);

        // When
        SubmitResponse<State> result = underTest.submit(payload);

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

        PossessionClaimResponse response = PossessionClaimResponse.builder()
            .defendantContactDetails(contactDetails)
            .defendantResponses(DefendantResponses.builder().build())
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
        DefendantContactDetails savedContactDetails = savedDraft.getPossessionClaimResponse()
            .getDefendantContactDetails();
        assertThat(savedContactDetails).isNotNull();
        assertThat(savedContactDetails.getParty()).isNotNull();
        assertThat(savedContactDetails.getParty().getFirstName()).isEqualTo("John");
        assertThat(savedContactDetails.getParty().getLastName()).isEqualTo("Doe");
        assertThat(savedContactDetails.getParty().getEmailAddress())
            .isEqualTo("john@example.com");
        assertThat(savedContactDetails.getParty().getPhoneNumber())
            .isEqualTo("07700900000");
        assertThat(savedContactDetails.getParty().getAddress()).isEqualTo(address);
        // Note: submitDraftAnswers is NOT persisted to draft - it's a transient UI flag
    }

    @Test
    void shouldSaveContactPreferencesWhenSubmitDraftAnswersIsYes() {
        // Given
        Party party = Party.builder()
            .firstName("John")
            .lastName("Doe")
            .phoneNumber("07123456789")
            .emailAddress("john@example.com")
            .address(AddressUK.builder().build())
            .build();

        DefendantContactDetails defendantContactDetails =
            DefendantContactDetails.builder()
                .party(party)
                .build();

        PossessionClaimResponse response = PossessionClaimResponse.builder()
            .defendantContactDetails(defendantContactDetails)
            .build();

        PCSCase caseData = PCSCase.builder()
            .possessionClaimResponse(response)
            .submitDraftAnswers(YesOrNo.YES)
            .build();

        when(draftCaseDataService.getUnsubmittedCaseData(CASE_REFERENCE, respondPossessionClaim))
            .thenReturn(Optional.of(caseData));

        EventPayload<PCSCase, State> eventPayload = createEventPayload(caseData);

        // When
        SubmitResponse<State> result = underTest.submit(eventPayload);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getErrors()).isNullOrEmpty();

        // Verify contact preferences service was called
        verify(possessionClaimResponsePersistenceService).saveDraftData(response);

        // Verify draft service was called once
        verify(draftCaseDataService, times(1))
            .patchUnsubmittedEventData(eq(CASE_REFERENCE),
                any(),
                eq(EventId.respondPossessionClaim));
    }

    @Test
    void shouldNotSaveContactPreferencesWhenSubmitDraftAnswersIsNo() {
        // Given
        Party party = Party.builder()
            .firstName("John")
            .lastName("Doe")
            .phoneNumber("07123456789")
            .emailAddress("john@example.com")
            .address(AddressUK.builder().build())
            .build();

        DefendantContactDetails defendantContactDetails =
            DefendantContactDetails.builder()
            .party(party)
            .build();

        PossessionClaimResponse response = PossessionClaimResponse.builder()
            .defendantContactDetails(defendantContactDetails)
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

        // Verify contact preferences service was NOT called
        verify(possessionClaimResponsePersistenceService, never()).saveDraftData(any(PossessionClaimResponse.class));

        // Verify draft service was called once
        verify(draftCaseDataService, times(1))
            .patchUnsubmittedEventData(eq(CASE_REFERENCE), any(), eq(respondPossessionClaim));
    }

    @Test
    void shouldSaveAllContactPreferenceFieldsWhenFinalSubmit() {
        // Given - All contact preference fields populated
        AddressUK address = AddressUK.builder().addressLine1("123 Test Street").build();

        Party party = Party.builder()
            .firstName("Jane")
            .lastName("Smith")
            .phoneNumber("07987654321")
            .emailAddress("jane.smith@example.com")
            .address(address)
            .build();

        DefendantContactDetails defendantContactDetails =
            DefendantContactDetails.builder()
                .party(party)
                .build();

        DefendantResponses defendantResponses =
            DefendantResponses.builder()
                .contactByEmail(VerticalYesNo.NO)
                .contactByText(VerticalYesNo.YES)
                .contactByPost(VerticalYesNo.NO)
                .contactByPhone(VerticalYesNo.YES)
                .build();

        PossessionClaimResponse response = PossessionClaimResponse.builder()
            .defendantContactDetails(defendantContactDetails)
            .defendantResponses(defendantResponses)
            .build();

        PCSCase caseData = PCSCase.builder()
            .possessionClaimResponse(response)
            .submitDraftAnswers(YesOrNo.YES)
            .build();

        EventPayload<PCSCase, State> eventPayload = createEventPayload(caseData);

        // When
        when(draftCaseDataService.getUnsubmittedCaseData(CASE_REFERENCE, respondPossessionClaim))
            .thenReturn(Optional.of(caseData));
        SubmitResponse<State> result = underTest.submit(eventPayload);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getErrors()).isNullOrEmpty();

        // Verify the exact response object is passed to the service
        ArgumentCaptor<PossessionClaimResponse> responseCaptor =
            ArgumentCaptor.forClass(PossessionClaimResponse.class);
        verify(possessionClaimResponsePersistenceService).saveDraftData(responseCaptor.capture());

        PossessionClaimResponse capturedResponse = responseCaptor.getValue();
        assertThat(capturedResponse.getDefendantResponses().getContactByEmail()).isEqualTo(VerticalYesNo.NO);
        assertThat(capturedResponse.getDefendantResponses().getContactByText()).isEqualTo(VerticalYesNo.YES);
        assertThat(capturedResponse.getDefendantResponses().getContactByPost()).isEqualTo(VerticalYesNo.NO);
        assertThat(capturedResponse.getDefendantResponses().getContactByPhone()).isEqualTo(VerticalYesNo.YES);
        assertThat(capturedResponse.getDefendantContactDetails().getParty().getPhoneNumber())
            .isEqualTo("07987654321");
        assertThat(capturedResponse.getDefendantContactDetails().getParty().getEmailAddress())
            .isEqualTo("jane.smith@example.com");
        assertThat(capturedResponse.getDefendantContactDetails().getParty().getAddress().getAddressLine1())
            .isEqualTo("123 Test Street");
    }

    @Test
    void shouldHandleContactPreferencesWithNullValues() {
        // Given - Some null preferences

        DefendantResponses defendantResponses =
            DefendantResponses.builder()
                .contactByEmail(null)
                .contactByText(VerticalYesNo.YES)
                .contactByPost(null)
                .contactByPhone(VerticalYesNo.YES)
                .build();

        PossessionClaimResponse response = PossessionClaimResponse.builder()
            .defendantResponses(defendantResponses)
            .build();

        PCSCase caseData = PCSCase.builder()
            .possessionClaimResponse(response)
            .submitDraftAnswers(YesOrNo.YES)
            .build();

        EventPayload<PCSCase, State> eventPayload = createEventPayload(caseData);

        // When
        when(draftCaseDataService.getUnsubmittedCaseData(CASE_REFERENCE, respondPossessionClaim))
            .thenReturn(Optional.of(caseData));

        SubmitResponse<State> result = underTest.submit(eventPayload);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getErrors()).isNullOrEmpty();

        // Verify service handles null values gracefully
        verify(possessionClaimResponsePersistenceService).saveDraftData(response);
    }

    @Test
    void shouldHandleExceptionFromContactPreferencesService() {
        // Given
        Party party = Party.builder()
            .firstName("Test")
            .lastName("User")
            .build();

        DefendantContactDetails defendantContactDetails = DefendantContactDetails.builder()
            .party(party).build();

        DefendantResponses defendantResponses = DefendantResponses.builder()
            .contactByPhone(VerticalYesNo.YES)
            .build();

        PossessionClaimResponse response = PossessionClaimResponse.builder()
            .defendantContactDetails(defendantContactDetails)
            .defendantResponses(defendantResponses)
            .build();

        PCSCase caseData = PCSCase.builder()
            .possessionClaimResponse(response)
            .submitDraftAnswers(YesOrNo.YES)
            .build();

        EventPayload<PCSCase, State> eventPayload = createEventPayload(caseData);

        //when
        when(draftCaseDataService.getUnsubmittedCaseData(CASE_REFERENCE, respondPossessionClaim))
            .thenReturn(Optional.of(caseData));

        // Mock service to throw exception
        doThrow(new IllegalStateException("No party found for IDAM ID"))
            .when(possessionClaimResponsePersistenceService).saveDraftData(any());

        // When / Then - Exception should propagate (not caught by handler)
        // This tests that the handler doesn't swallow exceptions
        assertThat(org.junit.jupiter.api.Assertions.assertThrows(
            IllegalStateException.class,
            () -> underTest.submit(eventPayload)
        )).hasMessage("No party found for IDAM ID");

        verify(possessionClaimResponsePersistenceService).saveDraftData(response);
    }

    @Test
    void shouldCallContactPreferencesServiceBeforeReturningSuccess() {
        // Given
        Party party = Party.builder()
            .firstName("Test")
            .lastName("User")
            .build();

        DefendantContactDetails defendantContactDetails = DefendantContactDetails.builder()
            .party(party).build();

        DefendantResponses defendantResponses = DefendantResponses.builder()
            .contactByPhone(VerticalYesNo.YES)
            .build();

        PossessionClaimResponse response = PossessionClaimResponse.builder()
            .defendantContactDetails(defendantContactDetails)
            .defendantResponses(defendantResponses)
            .build();

        PCSCase caseData = PCSCase.builder()
            .possessionClaimResponse(response)
            .submitDraftAnswers(YesOrNo.YES)
            .build();

        EventPayload<PCSCase, State> eventPayload = createEventPayload(caseData);

        when(draftCaseDataService.getUnsubmittedCaseData(CASE_REFERENCE, respondPossessionClaim))
            .thenReturn(Optional.of(caseData));

        // When
        SubmitResponse<State> result = underTest.submit(eventPayload);

        // Then - Verify service is called and success is returned
        verify(possessionClaimResponsePersistenceService).saveDraftData(response);
        assertThat(result).isNotNull();
        assertThat(result.getErrors()).isNullOrEmpty();
        assertThat(result.getState()).isNull(); // Default response has null state
    }

    @Test
    void shouldNotCallContactPreferencesServiceWhenValidationFails() {
        // Given - Missing possessionClaimResponse
        PCSCase caseData = PCSCase.builder()
            .possessionClaimResponse(null)  // Will fail validation
            .submitDraftAnswers(YesOrNo.YES)
            .build();

        EventPayload<PCSCase, State> eventPayload = createEventPayload(caseData);

        // When
        SubmitResponse<State> result = underTest.submit(eventPayload);

        // Then - Service should NOT be called due to validation failure
        verify(possessionClaimResponsePersistenceService, never()).saveDraftData(any());
        assertThat(result.getErrors()).isNotEmpty();
    }

    private EventPayload<PCSCase, State> createEventPayload(PCSCase caseData) {
        when(eventPayload.caseReference()).thenReturn(CASE_REFERENCE);
        when(eventPayload.caseData()).thenReturn(caseData);
        return eventPayload;
    }

    private PCSCase createDraftSaveCaseData(DefendantContactDetails contactDetails, DefendantResponses responses) {
        PossessionClaimResponse response = PossessionClaimResponse.builder()
            .defendantContactDetails(contactDetails)
            .defendantResponses(responses != null ? responses : DefendantResponses.builder().build())
            .build();

        return PCSCase.builder()
            .possessionClaimResponse(response)
            .submitDraftAnswers(YesOrNo.NO)
            .build();
    }

    private void submitAndVerifyDraftSaved(PCSCase caseData) {
        EventPayload<PCSCase, State> payload = createEventPayload(caseData);
        SubmitResponse<State> result = underTest.submit(payload);

        assertThat(result).isNotNull();
        assertThat(result.getErrors()).isNullOrEmpty();

        verify(draftCaseDataService).patchUnsubmittedEventData(
            eq(CASE_REFERENCE), any(PCSCase.class), eq(respondPossessionClaim)
        );
    }
}
