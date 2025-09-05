package uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.Event;
import uk.gov.hmcts.ccd.sdk.api.callback.MidEvent;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentPaymentFrequency;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;

class RentDetailsTest extends BasePageTest {

    private Event<PCSCase, UserRole, State> event;

    @BeforeEach
    void setUp() {
        event = buildPageInTestEvent(new RentDetails());
    }

    @Test
    void shouldCalculateDailyRentForWeeklyFrequency() {
        // Given
        CaseDetails<PCSCase, State> caseDetails = new CaseDetails<>();
        PCSCase caseData = PCSCase.builder()
                .currentRent("7000") // £70.00 in pence
                .rentFrequency(RentPaymentFrequency.WEEKLY)
                .build();
        caseDetails.setData(caseData);

        // When
        MidEvent<PCSCase, State> midEvent = getMidEventForPage(event, "rentDetails");
        midEvent.handle(caseDetails, null);

        // Then
        assertThat(caseData.getCalculatedDailyRentChargeAmount()).isEqualTo("1000"); // £10.00 per day
    }

    @Test
    void shouldCalculateDailyRentForMonthlyFrequency() {
        // Given
        CaseDetails<PCSCase, State> caseDetails = new CaseDetails<>();
        PCSCase caseData = PCSCase.builder()
                .currentRent("30000") // £300.00 in pence
                .rentFrequency(RentPaymentFrequency.MONTHLY)
                .build();
        caseDetails.setData(caseData);

        // When
        MidEvent<PCSCase, State> midEvent = getMidEventForPage(event, "rentDetails");
        midEvent.handle(caseDetails, null);

        // Then
        assertThat(caseData.getCalculatedDailyRentChargeAmount()).isEqualTo("986"); // £9.86 per day
    }

    @Test
    void shouldUseProvidedDailyRentForOtherFrequency() {
        // Given
        CaseDetails<PCSCase, State> caseDetails = new CaseDetails<>();
        PCSCase caseData = PCSCase.builder()
                .rentFrequency(RentPaymentFrequency.OTHER)
                .dailyRentChargeAmount("1500") // £15.00 per day
                .build();
        caseDetails.setData(caseData);

        // When
        MidEvent<PCSCase, State> midEvent = getMidEventForPage(event, "rentDetails");
        midEvent.handle(caseDetails, null);

        // Then
        assertThat(caseData.getDailyRentChargeAmount()).isEqualTo("1500");
    }

    @Test
    void shouldReturnErrorWhenCurrentRentIsNegative() {
        // Given
        CaseDetails<PCSCase, State> caseDetails = new CaseDetails<>();
        PCSCase caseData = PCSCase.builder()
                .currentRent("-1000")
                .rentFrequency(RentPaymentFrequency.MONTHLY)
                .build();
        caseDetails.setData(caseData);

        // When
        MidEvent<PCSCase, State> midEvent = getMidEventForPage(event, "rentDetails");
        var response = midEvent.handle(caseDetails, null);

        // Then
        assertThat(response.getErrors()).containsExactly("Rent amount cannot be negative");
    }

    @Test
    void shouldReturnErrorWhenDailyRentChargeAmountIsNegative() {
        // Given
        CaseDetails<PCSCase, State> caseDetails = new CaseDetails<>();
        PCSCase caseData = PCSCase.builder()
                .rentFrequency(RentPaymentFrequency.OTHER)
                .dailyRentChargeAmount("-500")
                .build();
        caseDetails.setData(caseData);

        // When
        MidEvent<PCSCase, State> midEvent = getMidEventForPage(event, "rentDetails");
        var response = midEvent.handle(caseDetails, null);

        // Then
        assertThat(response.getErrors()).containsExactly("Daily rent charge amount cannot be negative");
    }
}
