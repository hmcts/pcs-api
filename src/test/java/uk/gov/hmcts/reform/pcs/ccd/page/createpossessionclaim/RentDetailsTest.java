package uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentPaymentFrequency;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;

import static org.assertj.core.api.Assertions.assertThat;

class RentDetailsTest extends BasePageTest {

    @BeforeEach
    void setUp() {
        setPageUnderTest(new RentDetails());
    }

    @Test
    void shouldCalculateDailyRentForWeeklyFrequency() {
        // Given
        PCSCase caseData = PCSCase.builder()
                .currentRent("7000") // £70.00 in pence
                .rentFrequency(RentPaymentFrequency.WEEKLY)
                .build();

        // When
        callMidEventHandler(caseData);

        // Then
        assertThat(caseData.getCalculatedDailyRentChargeAmount()).isEqualTo("1000"); // £10.00 per day
    }

    @Test
    void shouldCalculateDailyRentForMonthlyFrequency() {
        // Given
        PCSCase caseData = PCSCase.builder()
                .currentRent("30000") // £300.00 in pence
                .rentFrequency(RentPaymentFrequency.MONTHLY)
                .build();

        // When
        callMidEventHandler(caseData);

        // Then
        assertThat(caseData.getCalculatedDailyRentChargeAmount()).isEqualTo("986"); // £9.86 per day
    }

    @Test
    void shouldUseProvidedDailyRentForOtherFrequency() {
        // Given
        PCSCase caseData = PCSCase.builder()
                .rentFrequency(RentPaymentFrequency.OTHER)
                .dailyRentChargeAmount("1500") // £15.00 per day
                .build();

        // When
        callMidEventHandler(caseData);

        // Then
        assertThat(caseData.getDailyRentChargeAmount()).isEqualTo("1500");
    }
}
