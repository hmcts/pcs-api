package uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.breathingspace;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.breathingspace.BreathingSpaceDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.breathingspace.BreathingSpaceType;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.breathingspace.BreathingSpaceApplicationPage.FUTURE_START_DATE_ERROR;

public class BreathingSpaceApplicationPageTest extends BasePageTest {

    private static final Clock UK_CLOCK = Clock.fixed(
        Instant.parse("2026-07-16T10:00:00Z"),
        ZoneId.of("Europe/London")
    );

    @BeforeEach
    void setUp() {
        setPageUnderTest(new BreathingSpaceApplicationPage(UK_CLOCK));
    }

    @Test
    void shouldReturnErrorWhenStartDateIsInFuture() {
        PCSCase caseData = caseDataWithStartDate(LocalDate.of(2026, 7, 17));
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);
        assertThat(response.getErrorMessageOverride()).isEqualTo(FUTURE_START_DATE_ERROR);
    }

    @Test
    void shouldAllowStartDateOfPast() {
        PCSCase caseData = caseDataWithStartDate(LocalDate.parse("2026-07-16"));
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);
        assertThat(response.getErrorMessageOverride()).isNull();
    }

    @Test
    void shouldAllowStartDateOfToday() {
        PCSCase caseData = caseDataWithStartDate(LocalDate.now(UK_CLOCK));
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);
        assertThat(response.getErrorMessageOverride()).isNull();
    }

    @Test
    void shouldAllowMissingStartDate() {
        PCSCase caseData = caseDataWithStartDate(null);
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);
        assertThat(response.getErrorMessageOverride()).isNull();
    }

    @Test
    void shouldAllowMissingBreathingSpaceDetails() {
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(PCSCase.builder().build());
        assertThat(response.getErrorMessageOverride()).isNull();
    }

    private static PCSCase caseDataWithStartDate(LocalDate startDate) {
        return PCSCase.builder()
            .breathingSpaceDetails(BreathingSpaceDetails.builder()
                .startDate(startDate)
                .type(BreathingSpaceType.MENTAL_HEALTH_CRISIS_MORATORIUM)
                .build())
            .build();
    }

}
