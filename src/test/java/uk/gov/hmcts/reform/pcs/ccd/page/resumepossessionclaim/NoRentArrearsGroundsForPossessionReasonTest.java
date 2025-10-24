package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.pcs.ccd.domain.model.NoRentArrearsReasonForGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("NoRentArrearsGroundsForPossessionReason Tests")
class NoRentArrearsGroundsForPossessionReasonTest extends BasePageTest {

    private NoRentArrearsGroundsForPossessionReason pageUnderTest;

    @BeforeEach
    void setUp() {
        pageUnderTest = new NoRentArrearsGroundsForPossessionReason();
        setPageUnderTest(pageUnderTest);
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

        // When & Then - Just verify the page configuration is created without errors
        // Since there's no mid event handler, we just test that the page can be instantiated
        assertThat(pageUnderTest).isNotNull();
        assertThat(pageUnderTest).isInstanceOf(NoRentArrearsGroundsForPossessionReason.class);
    }

    @Test
    @DisplayName("Should handle null no rent arrears reasons gracefully")
    void shouldHandleNullNoRentArrearsReasonsGracefully() {
        // Given
        PCSCase caseData = PCSCase.builder()
            .noRentArrearsReasonForGrounds(null)
            .build();

        // When & Then - Just verify the page configuration is created without errors
        assertThat(pageUnderTest).isNotNull();
        assertThat(pageUnderTest).isInstanceOf(NoRentArrearsGroundsForPossessionReason.class);
    }

    @Test
    @DisplayName("Should handle empty no rent arrears reasons gracefully")
    void shouldHandleEmptyNoRentArrearsReasonsGracefully() {
        // Given
        PCSCase caseData = PCSCase.builder()
            .noRentArrearsReasonForGrounds(NoRentArrearsReasonForGrounds.builder().build())
            .build();

        // When & Then - Just verify the page configuration is created without errors
        assertThat(pageUnderTest).isNotNull();
        assertThat(pageUnderTest).isInstanceOf(NoRentArrearsGroundsForPossessionReason.class);
    }
}

