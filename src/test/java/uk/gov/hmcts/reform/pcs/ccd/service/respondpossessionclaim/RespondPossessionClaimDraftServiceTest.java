package uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.ccd.domain.PossessionClaimResponse;
import uk.gov.hmcts.reform.pcs.ccd.service.DraftCaseDataService;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.respondPossessionClaim;

@ExtendWith(MockitoExtension.class)
class RespondPossessionClaimDraftServiceTest {

    private static final long CASE_REFERENCE = 1234567890L;

    @Mock
    private DraftCaseDataService draftCaseDataService;

    @Captor
    private ArgumentCaptor<PCSCase> pcsCaseCaptor;

    private RespondPossessionClaimDraftService underTest;

    @BeforeEach
    void setUp() {
        underTest = new RespondPossessionClaimDraftService(draftCaseDataService);
    }

    @Test
    void shouldReturnTrueWhenDraftExists() {
        // Given
        when(draftCaseDataService.hasUnsubmittedCaseData(CASE_REFERENCE, respondPossessionClaim))
            .thenReturn(true);

        // When
        boolean result = underTest.exists(CASE_REFERENCE);

        // Then
        assertThat(result).isTrue();
        verify(draftCaseDataService).hasUnsubmittedCaseData(CASE_REFERENCE, respondPossessionClaim);
    }

    @Test
    void shouldReturnFalseWhenDraftDoesNotExist() {
        // Given
        when(draftCaseDataService.hasUnsubmittedCaseData(CASE_REFERENCE, respondPossessionClaim))
            .thenReturn(false);

        // When
        boolean result = underTest.exists(CASE_REFERENCE);

        // Then
        assertThat(result).isFalse();
        verify(draftCaseDataService).hasUnsubmittedCaseData(CASE_REFERENCE, respondPossessionClaim);
    }

    @Test
    void shouldLoadExistingDraftAndMergeWithPayloadData() {
        // Given - Draft data from database
        Party draftParty = Party.builder()
            .firstName("John")
            .lastName("Doe")
            .emailAddress("john@example.com")
            .build();

        PossessionClaimResponse draftResponse = PossessionClaimResponse.builder()
            .party(draftParty)
            .contactByPhone(YesOrNo.YES)
            .build();

        PCSCase draftData = PCSCase.builder()
            .possessionClaimResponse(draftResponse)
            .submitDraftAnswers(YesOrNo.YES)
            .hasUnsubmittedCaseData(YesOrNo.YES)
            .build();

        // Given - Payload data from CCD (contains fields like legislativeCountry)
        PCSCase payloadData = PCSCase.builder()
            .legislativeCountry(LegislativeCountry.ENGLAND)
            .formattedPropertyAddress("123 Test Street, London")
            .feeAmount("100.00")
            .build();

        when(draftCaseDataService.getUnsubmittedCaseData(CASE_REFERENCE, respondPossessionClaim))
            .thenReturn(Optional.of(draftData));

        // When
        PCSCase result = underTest.load(CASE_REFERENCE, payloadData);

        // Then - Verify draft data overlays onto payload
        assertThat(result).isNotNull();
        assertThat(result.getPossessionClaimResponse()).isEqualTo(draftResponse);
        assertThat(result.getPossessionClaimResponse().getParty().getFirstName()).isEqualTo("John");
        assertThat(result.getPossessionClaimResponse().getParty().getLastName()).isEqualTo("Doe");
        assertThat(result.getPossessionClaimResponse().getContactByPhone()).isEqualTo(YesOrNo.YES);
        assertThat(result.getSubmitDraftAnswers()).isEqualTo(YesOrNo.NO);
        assertThat(result.getHasUnsubmittedCaseData()).isEqualTo(YesOrNo.YES);

        // Then - Verify payload fields are preserved (no data loss from toBuilder)
        assertThat(result.getLegislativeCountry()).isEqualTo(LegislativeCountry.ENGLAND);
        assertThat(result.getFormattedPropertyAddress()).isEqualTo("123 Test Street, London");
        assertThat(result.getFeeAmount()).isEqualTo("100.00");

        // Then - Verify original payload is immutable (not mutated by toBuilder)
        assertThat(payloadData.getPossessionClaimResponse()).isNull();
        assertThat(payloadData.getHasUnsubmittedCaseData()).isNull();
        assertThat(payloadData.getSubmitDraftAnswers()).isNull();

        verify(draftCaseDataService).getUnsubmittedCaseData(CASE_REFERENCE, respondPossessionClaim);
    }

    @Test
    void shouldThrowIllegalStateExceptionWhenDraftNotFound() {
        // Given
        when(draftCaseDataService.getUnsubmittedCaseData(CASE_REFERENCE, respondPossessionClaim))
            .thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> underTest.load(CASE_REFERENCE, PCSCase.builder().build()))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Draft not found for case " + CASE_REFERENCE);
    }

    @Test
    void shouldInitializeNewDraftAndMergeWithPayloadData() {
        // Given - Initial response from database
        Party party = Party.builder()
            .firstName("Jane")
            .lastName("Smith")
            .build();

        PossessionClaimResponse initialResponse = PossessionClaimResponse.builder()
            .party(party)
            .build();

        // Given - Payload data from CCD (contains fields like legislativeCountry)
        PCSCase payloadData = PCSCase.builder()
            .legislativeCountry(LegislativeCountry.WALES)
            .formattedPropertyAddress("456 Property Lane, Cardiff")
            .feeAmount("200.00")
            .caseTitleMarkdown("Test Case Title")
            .build();

        // When
        PCSCase result = underTest.initialize(CASE_REFERENCE, initialResponse, payloadData);

        // Then - Verify initial response overlays onto payload
        assertThat(result).isNotNull();
        assertThat(result.getPossessionClaimResponse()).isEqualTo(initialResponse);
        assertThat(result.getSubmitDraftAnswers()).isEqualTo(YesOrNo.NO);

        // Then - Verify payload fields are preserved (no data loss from toBuilder)
        assertThat(result.getLegislativeCountry()).isEqualTo(LegislativeCountry.WALES);
        assertThat(result.getFormattedPropertyAddress()).isEqualTo("456 Property Lane, Cardiff");
        assertThat(result.getFeeAmount()).isEqualTo("200.00");
        assertThat(result.getCaseTitleMarkdown()).isEqualTo("Test Case Title");

        // Then - Verify original payload is immutable (not mutated by toBuilder)
        assertThat(payloadData.getPossessionClaimResponse()).isNull();
        assertThat(payloadData.getSubmitDraftAnswers()).isNull();

        // Then - Verify only filtered data is saved to database (NOT payload fields)
        verify(draftCaseDataService).patchUnsubmittedEventData(
            eq(CASE_REFERENCE),
            pcsCaseCaptor.capture(),
            eq(respondPossessionClaim)
        );

        PCSCase patchedDraft = pcsCaseCaptor.getValue();
        assertThat(patchedDraft.getPossessionClaimResponse()).isEqualTo(initialResponse);
        assertThat(patchedDraft.getSubmitDraftAnswers()).isNull();
        assertThat(patchedDraft.getLegislativeCountry()).isNull(); // Should NOT save payload fields
        assertThat(patchedDraft.getFormattedPropertyAddress()).isNull();
        assertThat(patchedDraft.getFeeAmount()).isNull();
    }

    @Test
    void shouldSaveDraftWithCorrectDataStructure() {
        // Given
        Party party = Party.builder()
            .firstName("John")
            .lastName("Doe")
            .emailAddress("john@example.com")
            .phoneNumber("07700900000")
            .build();

        PossessionClaimResponse response = PossessionClaimResponse.builder()
            .party(party)
            .contactByPhone(YesOrNo.YES)
            .build();

        PCSCase caseData = PCSCase.builder()
            .possessionClaimResponse(response)
            .submitDraftAnswers(YesOrNo.NO)
            .build();

        // When
        underTest.save(CASE_REFERENCE, caseData);

        // Then
        verify(draftCaseDataService).patchUnsubmittedEventData(
            eq(CASE_REFERENCE),
            pcsCaseCaptor.capture(),
            eq(respondPossessionClaim)
        );

        PCSCase savedDraft = pcsCaseCaptor.getValue();
        assertThat(savedDraft.getPossessionClaimResponse()).isNotNull();
        assertThat(savedDraft.getPossessionClaimResponse().getParty()).isNotNull();
        assertThat(savedDraft.getPossessionClaimResponse().getParty().getFirstName()).isEqualTo("John");
        assertThat(savedDraft.getPossessionClaimResponse().getParty().getLastName()).isEqualTo("Doe");
        assertThat(savedDraft.getPossessionClaimResponse().getParty().getEmailAddress()).isEqualTo("john@example.com");
        assertThat(savedDraft.getPossessionClaimResponse().getParty().getPhoneNumber()).isEqualTo("07700900000");
        assertThat(savedDraft.getPossessionClaimResponse().getContactByPhone()).isEqualTo(YesOrNo.YES);
        assertThat(savedDraft.getSubmitDraftAnswers()).isEqualTo(YesOrNo.NO);
    }

}
