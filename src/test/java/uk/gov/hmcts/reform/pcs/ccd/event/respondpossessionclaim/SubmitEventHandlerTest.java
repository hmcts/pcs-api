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
import uk.gov.hmcts.reform.pcs.ccd.domain.PossessionClaimResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim.RespondPossessionClaimDraftService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubmitEventHandlerTest {

    private static final long CASE_REFERENCE = 1234567890L;

    @Mock
    private RespondPossessionClaimDraftService draftService;
    @Mock
    private EventPayload<PCSCase, State> eventPayload;

    @Captor
    private ArgumentCaptor<PCSCase> pcsCaseCaptor;

    private SubmitEventHandler underTest;

    @BeforeEach
    void setUp() {
        underTest = new SubmitEventHandler(draftService);
    }

    @Test
    void shouldSaveDraftWhenSubmitDraftAnswersIsNo() {
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

        PossessionClaimResponse response = PossessionClaimResponse.builder()
            .party(party)
            .contactByPhone(YesOrNo.YES)
            .build();

        PCSCase caseData = PCSCase.builder()
            .possessionClaimResponse(response)
            .submitDraftAnswers(YesOrNo.NO)
            .build();

        EventPayload<PCSCase, State> eventPayload = createEventPayload(caseData);

        // When
        SubmitResponse<State> result = underTest.handle(eventPayload);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getErrors()).isNullOrEmpty();

        verify(draftService).save(eq(CASE_REFERENCE), pcsCaseCaptor.capture());

        PCSCase savedDraft = pcsCaseCaptor.getValue();
        assertThat(savedDraft.getPossessionClaimResponse()).isNotNull();
        assertThat(savedDraft.getPossessionClaimResponse().getParty().getFirstName()).isEqualTo("John");
        assertThat(savedDraft.getSubmitDraftAnswers()).isEqualTo(YesOrNo.NO);
    }

    @Test
    void shouldNotSaveDraftWhenSubmitDraftAnswersIsYes() {
        // Given
        Party party = Party.builder()
            .firstName("John")
            .lastName("Doe")
            .build();

        PossessionClaimResponse response = PossessionClaimResponse.builder()
            .party(party)
            .build();

        PCSCase caseData = PCSCase.builder()
            .possessionClaimResponse(response)
            .submitDraftAnswers(YesOrNo.YES)
            .build();

        EventPayload<PCSCase, State> eventPayload = createEventPayload(caseData);

        // When
        SubmitResponse<State> result = underTest.handle(eventPayload);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getErrors()).isNullOrEmpty();

        verify(draftService, never()).save(eq(CASE_REFERENCE), any(PCSCase.class));
    }

    @Test
    void shouldReturnErrorWhenPossessionClaimResponseIsNull() {
        // Given
        PCSCase caseData = PCSCase.builder()
            .possessionClaimResponse(null)
            .submitDraftAnswers(YesOrNo.NO)
            .build();

        EventPayload<PCSCase, State> eventPayload = createEventPayload(caseData);

        // When
        SubmitResponse<State> result = underTest.handle(eventPayload);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getErrors()).isNotNull();
        assertThat(result.getErrors()).hasSize(1);
        assertThat(result.getErrors().get(0)).isEqualTo("Invalid submission: missing response data");

        verify(draftService, never()).save(eq(CASE_REFERENCE), any(PCSCase.class));
    }

    @Test
    void shouldReturnErrorWhenSubmitDraftAnswersIsNull() {
        // Given
        Party party = Party.builder()
            .firstName("John")
            .build();

        PossessionClaimResponse response = PossessionClaimResponse.builder()
            .party(party)
            .build();

        PCSCase caseData = PCSCase.builder()
            .possessionClaimResponse(response)
            .submitDraftAnswers(null)
            .build();

        EventPayload<PCSCase, State> eventPayload = createEventPayload(caseData);

        // When
        SubmitResponse<State> result = underTest.handle(eventPayload);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getErrors()).isNotNull();
        assertThat(result.getErrors()).hasSize(1);
        assertThat(result.getErrors().get(0)).isEqualTo("Invalid submission: missing submit flag");

        verify(draftService, never()).save(eq(CASE_REFERENCE), any(PCSCase.class));
    }

    @Test
    void shouldReturnErrorWhenPartyIsNull() {
        // Given
        PossessionClaimResponse response = PossessionClaimResponse.builder()
            .party(null)
            .build();

        PCSCase caseData = PCSCase.builder()
            .possessionClaimResponse(response)
            .submitDraftAnswers(YesOrNo.NO)
            .build();

        EventPayload<PCSCase, State> eventPayload = createEventPayload(caseData);

        // When
        SubmitResponse<State> result = underTest.handle(eventPayload);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getErrors()).isNotNull();
        assertThat(result.getErrors()).hasSize(1);
        assertThat(result.getErrors().get(0))
            .isEqualTo("Invalid response structure. Please refresh the page and try again.");

        verify(draftService, never()).save(eq(CASE_REFERENCE), any(PCSCase.class));
    }

    @Test
    void shouldReturnErrorWhenDraftServiceThrowsException() {
        // Given
        Party party = Party.builder()
            .firstName("John")
            .lastName("Doe")
            .build();

        PossessionClaimResponse response = PossessionClaimResponse.builder()
            .party(party)
            .build();

        PCSCase caseData = PCSCase.builder()
            .possessionClaimResponse(response)
            .submitDraftAnswers(YesOrNo.NO)
            .build();

        EventPayload<PCSCase, State> eventPayload = createEventPayload(caseData);

        doThrow(new RuntimeException("Database connection failed"))
            .when(draftService).save(eq(CASE_REFERENCE), any(PCSCase.class));

        // When
        SubmitResponse<State> result = underTest.handle(eventPayload);

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

        PossessionClaimResponse response = PossessionClaimResponse.builder()
            .party(party)
            .build();

        PCSCase caseData = PCSCase.builder()
            .possessionClaimResponse(response)
            .submitDraftAnswers(YesOrNo.NO)
            .build();

        EventPayload<PCSCase, State> eventPayload = createEventPayload(caseData);

        // When
        SubmitResponse<State> result = underTest.handle(eventPayload);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getErrors()).isNullOrEmpty();

        verify(draftService).save(eq(CASE_REFERENCE), any(PCSCase.class));
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

        PossessionClaimResponse response = PossessionClaimResponse.builder()
            .party(party)
            .contactByPhone(YesOrNo.YES)
            .build();

        PCSCase caseData = PCSCase.builder()
            .possessionClaimResponse(response)
            .submitDraftAnswers(YesOrNo.NO)
            .build();

        EventPayload<PCSCase, State> eventPayload = createEventPayload(caseData);

        // When
        underTest.handle(eventPayload);

        // Then
        verify(draftService).save(eq(CASE_REFERENCE), pcsCaseCaptor.capture());

        PCSCase savedDraft = pcsCaseCaptor.getValue();
        assertThat(savedDraft.getPossessionClaimResponse()).isNotNull();
        assertThat(savedDraft.getPossessionClaimResponse().getParty()).isNotNull();
        assertThat(savedDraft.getPossessionClaimResponse().getParty().getFirstName()).isEqualTo("John");
        assertThat(savedDraft.getPossessionClaimResponse().getParty().getLastName()).isEqualTo("Doe");
        assertThat(savedDraft.getPossessionClaimResponse().getParty().getEmailAddress()).isEqualTo("john@example.com");
        assertThat(savedDraft.getPossessionClaimResponse().getParty().getPhoneNumber()).isEqualTo("07700900000");
        assertThat(savedDraft.getPossessionClaimResponse().getParty().getAddress()).isEqualTo(address);
        assertThat(savedDraft.getPossessionClaimResponse().getContactByPhone()).isEqualTo(YesOrNo.YES);
        assertThat(savedDraft.getSubmitDraftAnswers()).isEqualTo(YesOrNo.NO);
    }

    private EventPayload<PCSCase, State> createEventPayload(PCSCase caseData) {
        when(eventPayload.caseReference()).thenReturn(CASE_REFERENCE);
        when(eventPayload.caseData()).thenReturn(caseData);
        return eventPayload;
    }
}
