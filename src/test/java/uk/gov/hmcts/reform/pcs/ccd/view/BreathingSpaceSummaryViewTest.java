package uk.gov.hmcts.reform.pcs.ccd.view;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.breathingspace.BreathingSpaceDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.breathingspace.BreathingSpaceType;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.summary.BreathingSpaceTabDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.summary.SummaryTab;
import uk.gov.hmcts.reform.pcs.ccd.service.breathingspace.BreathingSpaceService;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.anyLong;

@ExtendWith(MockitoExtension.class)

class BreathingSpaceSummaryViewTest {
    private static final long CASE_REFERENCE = 1234L;

    @Mock
    private BreathingSpaceService breathingSpaceService;

    @InjectMocks
    private BreathingSpaceSummaryView underTest;

    @Test
    void shouldDisplayAllBreathingSpaceDetails() {
        // Given
        PCSCase pcsCase = buildCase();

        BreathingSpaceDetails details = BreathingSpaceDetails.builder()
            .referenceNumber("BS123456")
            .startDate(LocalDate.of(2026, 11, 12))
            .type(BreathingSpaceType.STANDARD)
            .expectedEndDate(LocalDate.of(2027, 1, 12))
            .build();

        when(breathingSpaceService.getLatestBreathingSpace(CASE_REFERENCE))
            .thenReturn(Optional.of(details));

        // When
        underTest.setCaseFields(
            pcsCase, CASE_REFERENCE, State.BREATHING_SPACE
        );

        // Then
        BreathingSpaceTabDetails result =
            pcsCase.getSummaryTab().getBreathingSpaceDetails();

        assertThat(result.getReferenceNumber()).isEqualTo("BS123456");
        assertThat(result.getStartDate()).isEqualTo("12/11/2026");
        assertThat(result.getType()).isEqualTo("Standard Breathing Space");
        assertThat(result.getExpectedEndDate()).isEqualTo("12/01/2027");
    }

    @Test
    void shouldDisplayNotProvidedForMissingOptionalFields() {
        // Given
        PCSCase pcsCase = buildCase();

        BreathingSpaceDetails details =
            BreathingSpaceDetails.builder().build();

        when(breathingSpaceService.getLatestBreathingSpace(CASE_REFERENCE))
            .thenReturn(Optional.of(details));

        // When
        underTest.setCaseFields(
            pcsCase, CASE_REFERENCE, State.BREATHING_SPACE
        );

        // Then
        BreathingSpaceTabDetails result =
            pcsCase.getSummaryTab().getBreathingSpaceDetails();

        assertThat(result.getReferenceNumber()).isEqualTo("Not provided");
        assertThat(result.getStartDate()).isEqualTo("Not provided");
        assertThat(result.getType()).isEqualTo("Not provided");
        assertThat(result.getExpectedEndDate()).isEqualTo("Not provided");
    }

    @Test
    void shouldNotDisplaySectionWhenNoRecordExists() {
        // Given
        PCSCase pcsCase = buildCase();

        when(breathingSpaceService.getLatestBreathingSpace(CASE_REFERENCE))
            .thenReturn(Optional.empty());

        // When
        underTest.setCaseFields(
            pcsCase, CASE_REFERENCE, State.BREATHING_SPACE
        );

        // Then
        assertThat(pcsCase.getSummaryTab().getBreathingSpaceDetails())
            .isNull();
    }

    @Test
    void shouldNotLoadDetailsWhenCaseIsNotInBreathingSpace() {
        // Given
        PCSCase pcsCase = buildCase();

        // When
        underTest.setCaseFields(
            pcsCase, CASE_REFERENCE, State.CASE_ISSUED
        );

        // Then
        assertThat(pcsCase.getSummaryTab().getBreathingSpaceDetails())
            .isNull();

        verify(breathingSpaceService, never())
            .getLatestBreathingSpace(anyLong());
    }

    @Test
    void shouldTreatBlankReferenceNumberAsNotProvided() {
        // Given
        PCSCase pcsCase = buildCase();

        BreathingSpaceDetails details = BreathingSpaceDetails.builder()
            .referenceNumber("   ")
            .build();

        when(breathingSpaceService.getLatestBreathingSpace(CASE_REFERENCE))
            .thenReturn(Optional.of(details));

        // When
        underTest.setCaseFields(
            pcsCase, CASE_REFERENCE, State.BREATHING_SPACE
        );

        // Then
        assertThat(pcsCase.getSummaryTab()
                       .getBreathingSpaceDetails()
                       .getReferenceNumber())
            .isEqualTo("Not provided");
    }

    private PCSCase buildCase() {
        return PCSCase.builder()
            .summaryTab(SummaryTab.builder().build())
            .build();
    }

}
