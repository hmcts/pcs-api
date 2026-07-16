package uk.gov.hmcts.reform.pcs.ccd.page.documentamend;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.documentamend.DocumentAmendDetails;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

class AmendDocumentDetailsPageTest extends BasePageTest {

    private static final Clock UK_CLOCK = Clock.fixed(
        Instant.parse("2026-07-16T10:00:00Z"),
        ZoneId.of("Europe/London")
    );

    @BeforeEach
    void setUp() {
        setPageUnderTest(new AmendDocumentDetailsPage(UK_CLOCK));
    }

    @Test
    void shouldReturnErrorWhenIssueDateIsInFuture() {
        PCSCase caseData = PCSCase.builder()
            .documentAmendDetails(DocumentAmendDetails.builder()
                .issueDate(LocalDate.of(2026, 7, 17))
                .build())
            .build();

        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        assertThat(response.getErrorMessageOverride()).isEqualTo("Issue date must be today or in the past");
    }

    @Test
    void shouldAllowIssueDateOfTodayOrPast() {
        PCSCase caseData = PCSCase.builder()
            .documentAmendDetails(DocumentAmendDetails.builder()
                .issueDate(LocalDate.of(2026, 7, 16))
                .build())
            .build();

        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        assertThat(response.getErrorMessageOverride()).isNull();
    }
}
