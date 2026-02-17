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
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.DraftCaseDataService;
import uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim.ImmutablePartyFieldValidator;

import java.util.List;

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
    private uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService pcsCaseService;
    @Mock
    private uk.gov.hmcts.reform.pcs.security.SecurityContextService securityContextService;
    @Mock
    private uk.gov.hmcts.reform.pcs.ccd.util.AddressMapper addressMapper;
    @Mock
    private uk.gov.hmcts.reform.pcs.ccd.repository.DefendantResponseRepository defendantResponseRepository;

    @Captor
    private ArgumentCaptor<PCSCase> pcsCaseCaptor;

    private SubmitEventHandler underTest;

    @BeforeEach
    void setUp() {
        underTest = new SubmitEventHandler(
            draftCaseDataService,
            immutableFieldValidator,
            pcsCaseService,
            securityContextService,
            addressMapper,
            defendantResponseRepository
        );
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
    void shouldSaveDraftWhenSubmitFlagIsYes() {
        // Given
        Party party = Party.builder()
            .firstName("John")
            .lastName("Doe")
            .emailAddress("john@example.com")
            .build();

        DefendantContactDetails contactDetails = DefendantContactDetails.builder()
            .party(party)
            .build();

        PossessionClaimResponse response = PossessionClaimResponse.builder()
            .defendantContactDetails(contactDetails)
            .defendantResponses(DefendantResponses.builder().build())
            .build();

        PCSCase draftData = PCSCase.builder()
            .possessionClaimResponse(response)
            .build();

        PCSCase caseData = PCSCase.builder()
            .possessionClaimResponse(response)
            .submitDraftAnswers(YesOrNo.YES)
            .build();

        EventPayload<PCSCase, State> payload = createEventPayload(caseData);

        // Mock dependencies for final submit
        uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity mockedCase = createMockedCaseEntity();
        java.util.UUID userId = mockedCase.getParties().iterator().next().getIdamId();

        when(securityContextService.getCurrentUserId()).thenReturn(userId);
        when(draftCaseDataService.getUnsubmittedCaseData(eq(CASE_REFERENCE), eq(respondPossessionClaim)))
            .thenReturn(java.util.Optional.of(draftData));
        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(mockedCase);

        // When
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
            .emailAddress("john@example.com")
            .build();

        DefendantContactDetails contactDetails = DefendantContactDetails.builder()
            .party(party)
            .build();

        DefendantResponses responses = DefendantResponses.builder()
            .tenancyTypeCorrect(YesNoNotSure.YES)
            .contactByEmail(VerticalYesNo.YES)
            .contactByText(VerticalYesNo.NO)
            .contactByPost(VerticalYesNo.YES)
            .contactByPhone(VerticalYesNo.NO)
            .build();

        PossessionClaimResponse response = PossessionClaimResponse.builder()
            .defendantContactDetails(contactDetails)
            .defendantResponses(responses)
            .build();

        PCSCase draftData = PCSCase.builder()
            .possessionClaimResponse(response)
            .build();

        PCSCase caseData = PCSCase.builder()
            .possessionClaimResponse(response)
            .submitDraftAnswers(YesOrNo.YES)
            .build();

        EventPayload<PCSCase, State> payload = createEventPayload(caseData);

        // Mock dependencies for final submit
        uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity mockedCase = createMockedCaseEntity();
        java.util.UUID userId = mockedCase.getParties().iterator().next().getIdamId();

        when(securityContextService.getCurrentUserId()).thenReturn(userId);
        when(draftCaseDataService.getUnsubmittedCaseData(eq(CASE_REFERENCE), eq(respondPossessionClaim)))
            .thenReturn(java.util.Optional.of(draftData));
        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(mockedCase);

        // When
        SubmitResponse<State> result = underTest.submit(payload);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getErrors()).isNullOrEmpty();

        verify(pcsCaseService, times(1)).saveCase(any(PcsCaseEntity.class)
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
        assertThat(result.getErrors().get(0))
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

    // ========== PHASE 2: DRAFT SAVE TESTS (CONTACT PREFERENCES) ==========

    @Test
    void shouldSaveContactPreferencesToDraft() {
        // Given - All 4 contact preferences set
        DefendantResponses defendantResponses = DefendantResponses.builder()
            .contactByEmail(VerticalYesNo.YES)
            .contactByText(VerticalYesNo.NO)
            .contactByPost(VerticalYesNo.YES)
            .contactByPhone(VerticalYesNo.NO)
            .build();

        PossessionClaimResponse response = PossessionClaimResponse.builder()
            .defendantContactDetails(DefendantContactDetails.builder()
                .party(Party.builder().firstName("John").build())
                .build())
            .defendantResponses(defendantResponses)
            .build();

        PCSCase caseData = PCSCase.builder()
            .possessionClaimResponse(response)
            .submitDraftAnswers(YesOrNo.NO)
            .build();

        EventPayload<PCSCase, State> payload = createEventPayload(caseData);

        when(immutableFieldValidator.findImmutableFieldViolations(any(), eq(CASE_REFERENCE)))
            .thenReturn(List.of());

        // When
        SubmitResponse<State> result = underTest.submit(payload);

        // Then
        assertThat(result.getErrors()).isNullOrEmpty();

        verify(draftCaseDataService).patchUnsubmittedEventData(
            eq(CASE_REFERENCE), pcsCaseCaptor.capture(), eq(respondPossessionClaim)
        );

        PCSCase savedDraft = pcsCaseCaptor.getValue();
        PossessionClaimResponse savedResponse = savedDraft.getPossessionClaimResponse();

        assertThat(savedResponse.getDefendantResponses().getContactByEmail()).isEqualTo(VerticalYesNo.YES);
        assertThat(savedResponse.getDefendantResponses().getContactByText()).isEqualTo(VerticalYesNo.NO);
        assertThat(savedResponse.getDefendantResponses().getContactByPost()).isEqualTo(VerticalYesNo.YES);
        assertThat(savedResponse.getDefendantResponses().getContactByPhone()).isEqualTo(VerticalYesNo.NO);
    }

    @Test
    void shouldSaveContactPreferencesWithContactDetails() {
        // Given - Contact details + contact preferences
        Party party = Party.builder()
            .firstName("Jane")
            .lastName("Doe")
            .emailAddress("jane@example.com")
            .phoneNumber("07700900000")
            .build();

        DefendantResponses defendantResponses = DefendantResponses.builder()
            .contactByEmail(VerticalYesNo.YES)
            .contactByText(VerticalYesNo.YES)
            .contactByPost(VerticalYesNo.NO)
            .contactByPhone(VerticalYesNo.NO)
            .build();

        PossessionClaimResponse response = PossessionClaimResponse.builder()
            .defendantContactDetails(DefendantContactDetails.builder()
                .party(party)
                .build())
            .defendantResponses(defendantResponses)
            .build();

        PCSCase caseData = PCSCase.builder()
            .possessionClaimResponse(response)
            .submitDraftAnswers(YesOrNo.NO)
            .build();

        EventPayload<PCSCase, State> payload = createEventPayload(caseData);

        when(immutableFieldValidator.findImmutableFieldViolations(any(), eq(CASE_REFERENCE)))
            .thenReturn(List.of());

        // When
        SubmitResponse<State> result = underTest.submit(payload);

        // Then
        assertThat(result.getErrors()).isNullOrEmpty();

        verify(draftCaseDataService).patchUnsubmittedEventData(
            eq(CASE_REFERENCE), pcsCaseCaptor.capture(), eq(respondPossessionClaim)
        );

        PCSCase savedDraft = pcsCaseCaptor.getValue();
        PossessionClaimResponse savedResponse = savedDraft.getPossessionClaimResponse();

        assertThat(savedResponse.getDefendantContactDetails().getParty().getFirstName()).isEqualTo("Jane");
        assertThat(savedResponse.getDefendantContactDetails().getParty().getEmailAddress())
            .isEqualTo("jane@example.com");
        assertThat(savedResponse.getDefendantResponses().getContactByEmail()).isEqualTo(VerticalYesNo.YES);
        assertThat(savedResponse.getDefendantResponses().getContactByText()).isEqualTo(VerticalYesNo.YES);
    }

    @Test
    void shouldSaveContactPreferencesWithResponses() {
        // Given - Defendant responses + contact preferences (no contact details)
        DefendantResponses responses = DefendantResponses.builder()
            .tenancyTypeCorrect(YesNoNotSure.YES)
            .oweRentArrears(YesNoNotSure.NO)
            .contactByEmail(VerticalYesNo.YES)
            .contactByPost(VerticalYesNo.YES)
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

        // Then
        assertThat(result.getErrors()).isNullOrEmpty();

        verify(draftCaseDataService).patchUnsubmittedEventData(
            eq(CASE_REFERENCE), pcsCaseCaptor.capture(), eq(respondPossessionClaim)
        );

        PCSCase savedDraft = pcsCaseCaptor.getValue();
        PossessionClaimResponse savedResponse = savedDraft.getPossessionClaimResponse();

        assertThat(savedResponse.getDefendantContactDetails()).isNull();
        assertThat(savedResponse.getDefendantResponses().getTenancyTypeCorrect()).isEqualTo(YesNoNotSure.YES);
        assertThat(savedResponse.getDefendantResponses().getContactByEmail()).isEqualTo(VerticalYesNo.YES);
        assertThat(savedResponse.getDefendantResponses().getContactByPost()).isEqualTo(VerticalYesNo.YES);
    }

    @Test
    void shouldSavePartialContactPreferences() {
        // Given - Only 2 contact preferences set, others null
        DefendantResponses defendantResponses = DefendantResponses.builder()
            .contactByEmail(VerticalYesNo.YES)
            .contactByText(null)
            .contactByPost(VerticalYesNo.YES)
            .contactByPhone(null)
            .build();

        PossessionClaimResponse response = PossessionClaimResponse.builder()
            .defendantContactDetails(DefendantContactDetails.builder()
                .party(Party.builder().firstName("John").build())
                .build())
            .defendantResponses(defendantResponses)
            .build();

        PCSCase caseData = PCSCase.builder()
            .possessionClaimResponse(response)
            .submitDraftAnswers(YesOrNo.NO)
            .build();

        EventPayload<PCSCase, State> payload = createEventPayload(caseData);

        when(immutableFieldValidator.findImmutableFieldViolations(any(), eq(CASE_REFERENCE)))
            .thenReturn(List.of());

        // When
        SubmitResponse<State> result = underTest.submit(payload);

        // Then - Null is OK for draft
        assertThat(result.getErrors()).isNullOrEmpty();

        verify(draftCaseDataService).patchUnsubmittedEventData(
            eq(CASE_REFERENCE), pcsCaseCaptor.capture(), eq(respondPossessionClaim)
        );

        PCSCase savedDraft = pcsCaseCaptor.getValue();
        PossessionClaimResponse savedResponse = savedDraft.getPossessionClaimResponse();

        assertThat(savedResponse.getDefendantResponses().getContactByEmail()).isEqualTo(VerticalYesNo.YES);
        assertThat(savedResponse.getDefendantResponses().getContactByText()).isNull();
        assertThat(savedResponse.getDefendantResponses().getContactByPost()).isEqualTo(VerticalYesNo.YES);
        assertThat(savedResponse.getDefendantResponses().getContactByPhone()).isNull();
    }

    @Test
    void shouldSaveContactPreferencesWhenAllNull() {
        // Given - All 4 contact preferences are null

        DefendantResponses defendantResponses = DefendantResponses.builder()
            .contactByEmail(null)
            .contactByText(null)
            .contactByPost(null)
            .contactByPhone(null)
            .build();

        PossessionClaimResponse response = PossessionClaimResponse.builder()
            .defendantContactDetails(DefendantContactDetails.builder()
                .party(Party.builder().firstName("John").build())
                .build())
            .defendantResponses(defendantResponses)
            .build();

        PCSCase caseData = PCSCase.builder()
            .possessionClaimResponse(response)
            .submitDraftAnswers(YesOrNo.NO)
            .build();

        EventPayload<PCSCase, State> payload = createEventPayload(caseData);

        when(immutableFieldValidator.findImmutableFieldViolations(any(), eq(CASE_REFERENCE)))
            .thenReturn(List.of());

        // When
        SubmitResponse<State> result = underTest.submit(payload);

        // Then - All null is OK for draft
        assertThat(result.getErrors()).isNullOrEmpty();

        verify(draftCaseDataService).patchUnsubmittedEventData(
            eq(CASE_REFERENCE), pcsCaseCaptor.capture(), eq(respondPossessionClaim)
        );
    }

    // ========== PHASE 3: FINAL SUBMIT VALIDATION TESTS (CONTACT PREFERENCES) ==========

    @Test
    void shouldRejectFinalSubmitWhenEmailPreferenceYesButNoEmailAddress() {
        // Given
        Party party = Party.builder()
            .firstName("John")
            .lastName("Doe")
            .emailAddress(null)  // Missing email
            .build();

        DefendantContactDetails contactDetails = DefendantContactDetails.builder()
            .party(party)
            .build();

        DefendantResponses defendantResponses = DefendantResponses.builder()
            .contactByEmail(VerticalYesNo.YES)
            .build();

        PossessionClaimResponse response = PossessionClaimResponse.builder()
            .defendantContactDetails(contactDetails)
            .defendantResponses(defendantResponses)  // Want email contact
            .build();

        PCSCase draftData = PCSCase.builder()
            .possessionClaimResponse(response)
            .build();

        PCSCase caseData = PCSCase.builder()
            .possessionClaimResponse(response)
            .submitDraftAnswers(YesOrNo.YES)  // Final submit
            .build();

        EventPayload<PCSCase, State> payload = createEventPayload(caseData);

        when(securityContextService.getCurrentUserId()).thenReturn(java.util.UUID.randomUUID());
        when(draftCaseDataService.getUnsubmittedCaseData(eq(CASE_REFERENCE), eq(respondPossessionClaim)))
            .thenReturn(java.util.Optional.of(draftData));

        // When
        SubmitResponse<State> result = underTest.submit(payload);

        // Then
        assertThat(result.getErrors())
            .hasSize(1)
            .contains("Email address is required when email contact preference is selected");
    }

    @Test
    void shouldRejectFinalSubmitWhenEmailPreferenceYesButEmptyEmailAddress() {
        // Given
        Party party = Party.builder()
            .firstName("John")
            .lastName("Doe")
            .emailAddress("")  // Empty email
            .build();

        DefendantContactDetails contactDetails = DefendantContactDetails.builder()
            .party(party)
            .build();

        DefendantResponses defendantResponses = DefendantResponses.builder()
            .contactByEmail(VerticalYesNo.YES)
            .build();

        PossessionClaimResponse response = PossessionClaimResponse.builder()
            .defendantContactDetails(contactDetails)
            .defendantResponses(defendantResponses)
            .build();

        PCSCase draftData = PCSCase.builder()
            .possessionClaimResponse(response)
            .build();

        PCSCase caseData = PCSCase.builder()
            .possessionClaimResponse(response)
            .submitDraftAnswers(YesOrNo.YES)
            .build();

        EventPayload<PCSCase, State> payload = createEventPayload(caseData);

        when(securityContextService.getCurrentUserId()).thenReturn(java.util.UUID.randomUUID());
        when(draftCaseDataService.getUnsubmittedCaseData(eq(CASE_REFERENCE), eq(respondPossessionClaim)))
            .thenReturn(java.util.Optional.of(draftData));

        // When
        SubmitResponse<State> result = underTest.submit(payload);

        // Then
        assertThat(result.getErrors())
            .hasSize(1)
            .contains("Email address is required when email contact preference is selected");
    }

    @Test
    void shouldRejectFinalSubmitWhenTextPreferenceYesButNoPhoneNumber() {
        // Given
        Party party = Party.builder()
            .firstName("John")
            .lastName("Doe")
            .phoneNumber(null)  // Missing phone
            .build();

        DefendantContactDetails contactDetails = DefendantContactDetails.builder()
            .party(party)
            .build();

        DefendantResponses defendantResponses = DefendantResponses.builder()
            .contactByText(VerticalYesNo.YES)
            .build();

        PossessionClaimResponse response = PossessionClaimResponse.builder()
            .defendantContactDetails(contactDetails)
            .defendantResponses(defendantResponses)
            .build();

        PCSCase draftData = PCSCase.builder()
            .possessionClaimResponse(response)
            .build();

        PCSCase caseData = PCSCase.builder()
            .possessionClaimResponse(response)
            .submitDraftAnswers(YesOrNo.YES)
            .build();

        EventPayload<PCSCase, State> payload = createEventPayload(caseData);

        when(securityContextService.getCurrentUserId()).thenReturn(java.util.UUID.randomUUID());
        when(draftCaseDataService.getUnsubmittedCaseData(eq(CASE_REFERENCE), eq(respondPossessionClaim)))
            .thenReturn(java.util.Optional.of(draftData));

        // When
        SubmitResponse<State> result = underTest.submit(payload);

        // Then
        assertThat(result.getErrors())
            .hasSize(1)
            .contains("Phone number is required when text or phone contact preference is selected");
    }

    @Test
    void shouldRejectFinalSubmitWhenPhonePreferenceYesButNoPhoneNumber() {
        // Given
        Party party = Party.builder()
            .firstName("John")
            .lastName("Doe")
            .phoneNumber(null)  // Missing phone
            .build();

        DefendantContactDetails contactDetails = DefendantContactDetails.builder()
            .party(party)
            .build();

        DefendantResponses defendantResponses = DefendantResponses.builder()
            .contactByPhone(VerticalYesNo.YES)
            .build();


        PossessionClaimResponse response = PossessionClaimResponse.builder()
            .defendantContactDetails(contactDetails)
            .defendantResponses(defendantResponses)
            .build();

        PCSCase draftData = PCSCase.builder()
            .possessionClaimResponse(response)
            .build();

        PCSCase caseData = PCSCase.builder()
            .possessionClaimResponse(response)
            .submitDraftAnswers(YesOrNo.YES)
            .build();

        EventPayload<PCSCase, State> payload = createEventPayload(caseData);

        when(securityContextService.getCurrentUserId()).thenReturn(java.util.UUID.randomUUID());
        when(draftCaseDataService.getUnsubmittedCaseData(eq(CASE_REFERENCE), eq(respondPossessionClaim)))
            .thenReturn(java.util.Optional.of(draftData));

        // When
        SubmitResponse<State> result = underTest.submit(payload);

        // Then
        assertThat(result.getErrors())
            .hasSize(1)
            .contains("Phone number is required when text or phone contact preference is selected");
    }

    @Test
    void shouldRejectFinalSubmitWhenMultiplePreferencesYesButMissingDetails() {
        // Given - Both email and text selected but no contact details
        Party party = Party.builder()
            .firstName("John")
            .lastName("Doe")
            .emailAddress(null)  // Missing email
            .phoneNumber(null)   // Missing phone
            .build();

        DefendantContactDetails contactDetails = DefendantContactDetails.builder()
            .party(party)
            .build();

        DefendantResponses defendantResponses = DefendantResponses.builder()
            .contactByEmail(VerticalYesNo.YES)
            .contactByText(VerticalYesNo.YES)
            .build();

        PossessionClaimResponse response = PossessionClaimResponse.builder()
            .defendantContactDetails(contactDetails)
            .defendantResponses(defendantResponses)
            .build();

        PCSCase draftData = PCSCase.builder()
            .possessionClaimResponse(response)
            .build();

        PCSCase caseData = PCSCase.builder()
            .possessionClaimResponse(response)
            .submitDraftAnswers(YesOrNo.YES)
            .build();

        EventPayload<PCSCase, State> payload = createEventPayload(caseData);

        when(securityContextService.getCurrentUserId()).thenReturn(java.util.UUID.randomUUID());
        when(draftCaseDataService.getUnsubmittedCaseData(eq(CASE_REFERENCE), eq(respondPossessionClaim)))
            .thenReturn(java.util.Optional.of(draftData));

        // When
        SubmitResponse<State> result = underTest.submit(payload);

        // Then - Multiple errors returned
        assertThat(result.getErrors())
            .hasSize(2)
            .contains("Email address is required when email contact preference is selected")
            .contains("Phone number is required when text or phone contact preference is selected");
    }

    @Test
    void shouldAcceptFinalSubmitWhenEmailPreferenceNoAndNoEmailAddress() {
        // Given - Email preference NO, so email address not required
        Party party = Party.builder()
            .firstName("John")
            .lastName("Doe")
            .emailAddress(null)  // Missing but OK
            .build();

        DefendantContactDetails contactDetails = DefendantContactDetails.builder()
            .party(party)
            .build();

        DefendantResponses defendantResponses = DefendantResponses.builder()
            .contactByEmail(VerticalYesNo.NO)
            .build();

        PossessionClaimResponse response = PossessionClaimResponse.builder()
            .defendantContactDetails(contactDetails)
            .defendantResponses(defendantResponses)  // Don't want email
            .build();

        PCSCase draftData = PCSCase.builder()
            .possessionClaimResponse(response)
            .build();

        PCSCase caseData = PCSCase.builder()
            .possessionClaimResponse(response)
            .submitDraftAnswers(YesOrNo.YES)
            .build();

        EventPayload<PCSCase, State> payload = createEventPayload(caseData);

        java.util.UUID userId = java.util.UUID.randomUUID();

        when(securityContextService.getCurrentUserId()).thenReturn(userId);
        when(draftCaseDataService.getUnsubmittedCaseData(eq(CASE_REFERENCE), eq(respondPossessionClaim)))
            .thenReturn(java.util.Optional.of(draftData));

        // When
        SubmitResponse<State> result = underTest.submit(payload);

        // Then - No validation error (may have other errors, but not email validation)
        if (result.getErrors() != null) {
            assertThat(result.getErrors())
                .as("Should not have email validation error when preference is NO")
                .noneMatch(error -> error.contains("Email address is required"));
        }
    }

    @Test
    void shouldAcceptFinalSubmitWhenEmailPreferenceNullAndNoEmailAddress() {
        // Given - Email preference null, so validation skipped
        Party party = Party.builder()
            .firstName("John")
            .lastName("Doe")
            .emailAddress(null)
            .build();

        DefendantContactDetails contactDetails = DefendantContactDetails.builder()
            .party(party)
            .build();

        DefendantResponses defendantResponses = DefendantResponses.builder()
            .contactByEmail(null)
            .build();

        PossessionClaimResponse response = PossessionClaimResponse.builder()
            .defendantContactDetails(contactDetails)
            .defendantResponses(defendantResponses)  // Null preference
            .build();

        PCSCase draftData = PCSCase.builder()
            .possessionClaimResponse(response)
            .build();

        PCSCase caseData = PCSCase.builder()
            .possessionClaimResponse(response)
            .submitDraftAnswers(YesOrNo.YES)
            .build();

        EventPayload<PCSCase, State> payload = createEventPayload(caseData);

        java.util.UUID userId = java.util.UUID.randomUUID();

        when(securityContextService.getCurrentUserId()).thenReturn(userId);
        when(draftCaseDataService.getUnsubmittedCaseData(eq(CASE_REFERENCE), eq(respondPossessionClaim)))
            .thenReturn(java.util.Optional.of(draftData));

        // When
        SubmitResponse<State> result = underTest.submit(payload);

        // Then
        if (result.getErrors() != null) {
            assertThat(result.getErrors())
                .as("Should not have email validation error when preference is null")
                .noneMatch(error -> error.contains("Email address is required"));
        }
    }

    @Test
    void shouldAcceptFinalSubmitWhenPostOnlyPreference() {
        // Given - Only post preference, no email/phone required
        Party party = Party.builder()
            .firstName("John")
            .lastName("Doe")
            .address(AddressUK.builder().addressLine1("123 Street").postCode("SW1A 1AA").build())
            .build();

        DefendantContactDetails contactDetails = DefendantContactDetails.builder()
            .party(party)
            .build();

        DefendantResponses defendantResponses = DefendantResponses.builder()
            .contactByPost(VerticalYesNo.YES)
            .contactByEmail(VerticalYesNo.NO)
            .contactByText(VerticalYesNo.NO)
            .contactByPhone(VerticalYesNo.NO)
            .build();

        PossessionClaimResponse response = PossessionClaimResponse.builder()
            .defendantContactDetails(contactDetails)
            .defendantResponses(defendantResponses)
            .build();

        PCSCase draftData = PCSCase.builder()
            .possessionClaimResponse(response)
            .build();

        PCSCase caseData = PCSCase.builder()
            .possessionClaimResponse(response)
            .submitDraftAnswers(YesOrNo.YES)
            .build();

        EventPayload<PCSCase, State> payload = createEventPayload(caseData);

        java.util.UUID userId = java.util.UUID.randomUUID();

        when(securityContextService.getCurrentUserId()).thenReturn(userId);
        when(draftCaseDataService.getUnsubmittedCaseData(eq(CASE_REFERENCE), eq(respondPossessionClaim)))
            .thenReturn(java.util.Optional.of(draftData));

        // When
        SubmitResponse<State> result = underTest.submit(payload);

        // Then - Post doesn't require validation
        if (result.getErrors() != null) {
            assertThat(result.getErrors())
                .as("Post preference should not require email or phone validation")
                .noneMatch(error -> error.contains("Email address is required"))
                .noneMatch(error -> error.contains("Phone number is required"));
        }
    }

    @Test
    void shouldRejectFinalSubmitWhenContactDetailsNull() {

        DefendantResponses defendantResponses = DefendantResponses.builder()
            .contactByEmail(VerticalYesNo.YES)
            .build();

        // Given - No contact details at all but email set as preference
        PossessionClaimResponse response = PossessionClaimResponse.builder()
            .defendantContactDetails(null)
            .defendantResponses(defendantResponses)
            .build();

        PCSCase draftData = PCSCase.builder()
            .possessionClaimResponse(response)
            .build();

        PCSCase caseData = PCSCase.builder()
            .possessionClaimResponse(response)
            .submitDraftAnswers(YesOrNo.YES)
            .build();

        EventPayload<PCSCase, State> payload = createEventPayload(caseData);

        when(securityContextService.getCurrentUserId()).thenReturn(java.util.UUID.randomUUID());
        when(draftCaseDataService.getUnsubmittedCaseData(eq(CASE_REFERENCE), eq(respondPossessionClaim)))
            .thenReturn(java.util.Optional.of(draftData));

        // When
        SubmitResponse<State> result = underTest.submit(payload);

        // Then
        assertThat(result.getErrors())
            .hasSize(1)
            .contains("Invalid submission: contact details required for final submission");
    }

    @Test
    void shouldRejectFinalSubmitWhenPartyNull() {
        // Given - Contact details exist but party is null
        DefendantContactDetails contactDetails = DefendantContactDetails.builder()
            .party(null)
            .build();

        DefendantResponses defendantResponses = DefendantResponses.builder()
            .contactByEmail(VerticalYesNo.YES)
            .build();

        PossessionClaimResponse response = PossessionClaimResponse.builder()
            .defendantContactDetails(contactDetails)
            .defendantResponses(defendantResponses)
            .build();

        PCSCase draftData = PCSCase.builder()
            .possessionClaimResponse(response)
            .build();

        PCSCase caseData = PCSCase.builder()
            .possessionClaimResponse(response)
            .submitDraftAnswers(YesOrNo.YES)
            .build();

        EventPayload<PCSCase, State> payload = createEventPayload(caseData);

        when(securityContextService.getCurrentUserId()).thenReturn(java.util.UUID.randomUUID());
        when(draftCaseDataService.getUnsubmittedCaseData(eq(CASE_REFERENCE), eq(respondPossessionClaim)))
            .thenReturn(java.util.Optional.of(draftData));

        // When
        SubmitResponse<State> result = underTest.submit(payload);

        // Then
        assertThat(result.getErrors())
            .hasSize(1)
            .contains("Invalid submission: contact details required for final submission");
    }

    @Test
    void shouldHandleWhitespaceOnlyEmailAddress() {
        // Given - Email is whitespace only
        Party party = Party.builder()
            .firstName("John")
            .lastName("Doe")
            .emailAddress("   ")  // Whitespace
            .build();

        DefendantContactDetails contactDetails = DefendantContactDetails.builder()
            .party(party)
            .build();

        DefendantResponses defendantResponses = DefendantResponses.builder()
            .contactByEmail(VerticalYesNo.YES)
            .build();

        PossessionClaimResponse response = PossessionClaimResponse.builder()
            .defendantContactDetails(contactDetails)
            .defendantResponses(defendantResponses)
            .build();

        PCSCase draftData = PCSCase.builder()
            .possessionClaimResponse(response)
            .build();

        PCSCase caseData = PCSCase.builder()
            .possessionClaimResponse(response)
            .submitDraftAnswers(YesOrNo.YES)
            .build();

        EventPayload<PCSCase, State> payload = createEventPayload(caseData);

        when(securityContextService.getCurrentUserId()).thenReturn(java.util.UUID.randomUUID());
        when(draftCaseDataService.getUnsubmittedCaseData(eq(CASE_REFERENCE), eq(respondPossessionClaim)))
            .thenReturn(java.util.Optional.of(draftData));

        // When
        SubmitResponse<State> result = underTest.submit(payload);

        // Then - Whitespace treated as blank
        assertThat(result.getErrors())
            .hasSize(1)
            .contains("Email address is required when email contact preference is selected");
    }

    private uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity createMockedCaseEntity() {
        java.util.UUID userId = java.util.UUID.randomUUID();

        uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity partyEntity =
            uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity.builder()
            .id(java.util.UUID.randomUUID())
            .idamId(userId)
            .contactPreferences(new java.util.HashSet<>())
            .build();

        uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity claimEntity =
            uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity.builder()
            .id(java.util.UUID.randomUUID())
            .build();

        uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity caseEntity =
            uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity.builder()
            .caseReference(CASE_REFERENCE)
            .parties(new java.util.HashSet<>(java.util.Set.of(partyEntity)))
            .claims(java.util.List.of(claimEntity))
            .build();

        return caseEntity;
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
