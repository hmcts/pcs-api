package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.model.NoRentArrearsReasonForGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("NoRentArrearsGroundsForPossessionReason Tests")
class NoRentArrearsGroundsForPossessionReasonTest extends BasePageTest {

    @BeforeEach
    void setUp() {
        setPageUnderTest(new NoRentArrearsGroundsForPossessionReason());
    }

    @Test
    @DisplayName("Should create page configuration successfully")
    void shouldCreatePageConfigurationSuccessfully() {
        // Given
        PCSCase caseData = PCSCase.builder()
            .noRentArrearsReasonForGrounds(NoRentArrearsReasonForGrounds.builder()
                .ownerOccupierTextArea("Owner occupier reason")
                .repossessionByLenderTextArea("Repossession reason")
                .holidayLetTextArea("Holiday let reason")
                .studentLetTextArea("Student let reason")
                .ministerOfReligionTextArea("Minister reason")
                .redevelopmentTextArea("Redevelopment reason")
                .build())
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getData()).isEqualTo(caseData);
        assertThat(response.getErrors()).isNullOrEmpty();
    }

    @Test
    @DisplayName("Should handle null no rent arrears reasons gracefully")
    void shouldHandleNullNoRentArrearsReasonsGracefully() {
        // Given
        PCSCase caseData = PCSCase.builder()
            .noRentArrearsReasonForGrounds(null)
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getData()).isEqualTo(caseData);
        assertThat(response.getErrors()).isNullOrEmpty();
    }

    @Test
    @DisplayName("Should handle empty no rent arrears reasons gracefully")
    void shouldHandleEmptyNoRentArrearsReasonsGracefully() {
        // Given
        PCSCase caseData = PCSCase.builder()
            .noRentArrearsReasonForGrounds(NoRentArrearsReasonForGrounds.builder().build())
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getData()).isEqualTo(caseData);
        assertThat(response.getErrors()).isNullOrEmpty();
    }
}

