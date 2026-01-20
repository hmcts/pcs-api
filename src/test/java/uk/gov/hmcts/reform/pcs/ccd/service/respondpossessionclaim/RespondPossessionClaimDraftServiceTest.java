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
    void shouldLoadExistingDraftAndSetSubmitDraftAnswersToNo() {
        // Given
        Party party = Party.builder()
            .firstName("John")
            .lastName("Doe")
            .emailAddress("john@example.com")
            .build();

        PossessionClaimResponse response = PossessionClaimResponse.builder()
            .party(party)
            .contactByPhone(YesOrNo.YES)
            .build();

        PCSCase draftData = PCSCase.builder()
            .possessionClaimResponse(response)
            .submitDraftAnswers(YesOrNo.YES)
            .build();

        when(draftCaseDataService.getUnsubmittedCaseData(CASE_REFERENCE, respondPossessionClaim))
            .thenReturn(Optional.of(draftData));

        // When
        PCSCase result = underTest.load(CASE_REFERENCE);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getPossessionClaimResponse()).isNotNull();
        assertThat(result.getPossessionClaimResponse().getParty()).isNotNull();
        assertThat(result.getPossessionClaimResponse().getParty().getFirstName()).isEqualTo("John");
        assertThat(result.getPossessionClaimResponse().getParty().getLastName()).isEqualTo("Doe");
        assertThat(result.getPossessionClaimResponse().getContactByPhone()).isEqualTo(YesOrNo.YES);
        assertThat(result.getSubmitDraftAnswers()).isEqualTo(YesOrNo.NO);

        verify(draftCaseDataService).getUnsubmittedCaseData(CASE_REFERENCE, respondPossessionClaim);
    }

    @Test
    void shouldThrowIllegalStateExceptionWhenDraftNotFound() {
        // Given
        when(draftCaseDataService.getUnsubmittedCaseData(CASE_REFERENCE, respondPossessionClaim))
            .thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> underTest.load(CASE_REFERENCE))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Draft not found for case " + CASE_REFERENCE);
    }

    @Test
    void shouldInitializeNewDraftAndPatchData() {
        // Given
        Party party = Party.builder()
            .firstName("Jane")
            .lastName("Smith")
            .build();

        PossessionClaimResponse initialResponse = PossessionClaimResponse.builder()
            .party(party)
            .build();

        // When
        PCSCase result = underTest.initialize(CASE_REFERENCE, initialResponse);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getPossessionClaimResponse()).isEqualTo(initialResponse);
        assertThat(result.getSubmitDraftAnswers()).isEqualTo(YesOrNo.NO);

        verify(draftCaseDataService).patchUnsubmittedEventData(
            eq(CASE_REFERENCE),
            pcsCaseCaptor.capture(),
            eq(respondPossessionClaim)
        );

        PCSCase patchedDraft = pcsCaseCaptor.getValue();
        assertThat(patchedDraft.getPossessionClaimResponse()).isEqualTo(initialResponse);
        assertThat(patchedDraft.getSubmitDraftAnswers()).isNull();
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
