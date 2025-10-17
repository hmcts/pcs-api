package uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.OccupationContractDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test class for OccupationContractDetails page configuration.
 */
class OccupationContractDetailsTest extends BasePageTest {

    private Clock ukClock;

    @BeforeEach
    void setUp() {
        ZonedDateTime fixedDateTime = ZonedDateTime.of(2024, 1, 15, 12, 0, 0, 0, ZoneId.of("Europe/London"));
        ukClock = Clock.fixed(fixedDateTime.toInstant(), ZoneId.of("Europe/London"));
        setPageUnderTest(new OccupationContractDetailsPage(ukClock));
    }

    @Test
    @DisplayName("Should allow progression when date field is not populated")
    void shouldAllowProgressionWhenDateFieldNotPopulated() {
        PCSCase caseData = PCSCase.builder()
            .occupationContractDetails(OccupationContractDetails.builder().build())
            .build();

        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        assertThat(response).isNotNull();
        assertThat(response.getErrors()).isNullOrEmpty();
    }

    @Test
    @DisplayName("Should show error when date is current date")
    void shouldShowErrorWhenDateIsCurrentDate() {
        LocalDate currentDate = LocalDate.of(2024, 1, 15);
        PCSCase caseData = PCSCase.builder()
            .occupationContractDetails(OccupationContractDetails.builder()
                .contractStartDate(currentDate)
                .build())
            .build();

        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        assertThat(response.getErrors()).contains("Occupation contract or licence start date cannot be today");
    }

    @Test
    @DisplayName("Should show error when date is in the future")
    void shouldShowErrorWhenDateIsInTheFuture() {
        LocalDate futureDate = LocalDate.of(2024, 1, 20);
        PCSCase caseData = PCSCase.builder()
            .occupationContractDetails(OccupationContractDetails.builder()
                .contractStartDate(futureDate)
                .build())
            .build();

        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        assertThat(response.getErrors()).contains("Occupation contract or licence start date cannot be in the future");
    }

    @Test
    @DisplayName("Should show error when date is not in the past")
    void shouldShowErrorWhenDateIsNotInThePast() {
        LocalDate currentDate = LocalDate.of(2024, 1, 15);
        PCSCase caseData = PCSCase.builder()
            .occupationContractDetails(OccupationContractDetails.builder()
                .contractStartDate(currentDate)
                .build())
            .build();

        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        assertThat(response.getErrors()).contains("Occupation contract or licence start date must be in the past");
    }

    @Test
    @DisplayName("Should allow valid past date")
    void shouldAllowValidPastDate() {
        LocalDate pastDate = LocalDate.of(2024, 1, 10);
        PCSCase caseData = PCSCase.builder()
            .occupationContractDetails(OccupationContractDetails.builder()
                .contractStartDate(pastDate)
                .build())
            .build();

        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        assertThat(response).isNotNull();
        assertThat(response.getErrors()).isNullOrEmpty();
    }

    @Test
    @DisplayName("Should allow progression when upload section is not populated")
    void shouldAllowProgressionWhenUploadSectionNotPopulated() {
        PCSCase caseData = PCSCase.builder()
            .occupationContractDetails(OccupationContractDetails.builder().build())
            .build();

        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        assertThat(response).isNotNull();
        assertThat(response.getErrors()).isNullOrEmpty();
    }

    @Test
    @DisplayName("Should handle null occupation contract details gracefully")
    void shouldHandleNullOccupationContractDetailsGracefully() {
        PCSCase caseData = PCSCase.builder()
            .occupationContractDetails(null)
            .build();

        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        assertThat(response).isNotNull();
        assertThat(response.getErrors()).isNullOrEmpty();
    }
}
