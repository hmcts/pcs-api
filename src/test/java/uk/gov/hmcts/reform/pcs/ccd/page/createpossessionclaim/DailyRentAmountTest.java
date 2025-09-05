package uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.Event;
import uk.gov.hmcts.ccd.sdk.api.callback.MidEvent;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;

class DailyRentAmountTest extends BasePageTest {

    private Event<PCSCase, UserRole, State> event;

    @BeforeEach
    void setUp() {
        event = buildPageInTestEvent(new DailyRentAmount());
    }

    @Test
    void shouldReturnErrorWhenAmendedDailyRentChargeAmountIsNegative() {
        // Given
        CaseDetails<PCSCase, State> caseDetails = new CaseDetails<>();
        PCSCase caseData = PCSCase.builder()
                .rentPerDayCorrect(VerticalYesNo.NO)
                .amendedDailyRentChargeAmount("-500")
                .build();
        caseDetails.setData(caseData);

        // When
        MidEvent<PCSCase, State> midEvent = getMidEventForPage(event, "dailyRentAmount");
        var response = midEvent.handle(caseDetails, null);

        // Then
        assertThat(response.getErrors()).containsExactly("Amended daily rent amount cannot be negative");
    }
}
