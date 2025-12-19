package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentPaymentFrequency;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;

import static org.assertj.core.api.Assertions.assertThat;

class RentDetailsTest extends BasePageTest {

    @BeforeEach
    void setUp() {
        setPageUnderTest(new RentDetailsPage());
    }

    @Test
    void shouldSetShowRentArrearsPageToNoForWeeklyFrequency() {
        // Given
        PCSCase caseData = PCSCase.builder()
                .rentDetails(RentDetails.builder()
                        .frequency(RentPaymentFrequency.WEEKLY)
                        .build())
                .build();

        // When
        callMidEventHandler(caseData);

        // Then
        assertThat(caseData.getShowRentArrearsPage()).isEqualTo(YesOrNo.NO);
    }

    @Test
    void shouldSetShowRentArrearsPageToNoForMonthlyFrequency() {
        // Given
        PCSCase caseData = PCSCase.builder()
                .rentDetails(RentDetails.builder()
                        .frequency(RentPaymentFrequency.MONTHLY)
                        .build())
                .build();

        // When
        callMidEventHandler(caseData);

        // Then
        assertThat(caseData.getShowRentArrearsPage()).isEqualTo(YesOrNo.NO);
    }

    @Test
    void shouldSetShowRentArrearsPageToNoForFortnightlyFrequency() {
        // Given
        PCSCase caseData = PCSCase.builder()
                .rentDetails(RentDetails.builder()
                        .frequency(RentPaymentFrequency.FORTNIGHTLY)
                        .build())
                .build();

        // When
        callMidEventHandler(caseData);

        // Then
        assertThat(caseData.getShowRentArrearsPage()).isEqualTo(YesOrNo.NO);
    }

    @Test
    void shouldSetShowRentArrearsPageToYesForOtherFrequency() {
        // Given
        PCSCase caseData = PCSCase.builder()
                .rentDetails(RentDetails.builder()
                        .frequency(RentPaymentFrequency.OTHER)
                        .build())
                .build();

        // When
        callMidEventHandler(caseData);

        // Then
        assertThat(caseData.getShowRentArrearsPage()).isEqualTo(YesOrNo.YES);
    }

    @Test
    void shouldNotProcessWhenRentFrequencyIsNull() {
        // Given
        PCSCase caseData = PCSCase.builder()
                .rentDetails(RentDetails.builder()
                        .frequency(null)
                        .build())
                .build();

        // When
        callMidEventHandler(caseData);

        // Then
        assertThat(caseData.getShowRentArrearsPage()).isNull();
    }

    @Test
    void shouldNotProcessWhenRentDetailsIsNull() {
        // Given
        PCSCase caseData = PCSCase.builder()
                .rentDetails(null)
                .build();

        // When
        callMidEventHandler(caseData);

        // Then
        assertThat(caseData.getShowRentArrearsPage()).isNull();
    }

    @Test
    void shouldSetShowRentArrearsPageWhenCurrentRentIsNull() {
        // Given
        PCSCase caseData = PCSCase.builder()
                .rentDetails(RentDetails.builder()
                        .currentRent(null)
                        .frequency(RentPaymentFrequency.WEEKLY)
                        .build())
                .build();

        // When
        callMidEventHandler(caseData);

        // Then
        assertThat(caseData.getShowRentArrearsPage()).isEqualTo(YesOrNo.NO);
    }

    @Test
    void shouldSetShowRentArrearsPageWhenCurrentRentIsEmpty() {
        // Given
        PCSCase caseData = PCSCase.builder()
                .rentDetails(RentDetails.builder()
                        .currentRent("")
                        .frequency(RentPaymentFrequency.WEEKLY)
                        .build())
                .build();

        // When
        callMidEventHandler(caseData);

        // Then
        assertThat(caseData.getShowRentArrearsPage()).isEqualTo(YesOrNo.NO);
    }

    @Test
    void shouldCalculateDailyRentForWeeklyFrequency() {
        // Given
        PCSCase caseData = PCSCase.builder()
                .rentDetails(RentDetails.builder()
                        .currentRent("30000") // £300.00 in pence
                        .frequency(RentPaymentFrequency.WEEKLY)
                        .build())
                .build();

        // When
        callMidEventHandler(caseData);

        // Then
        // 30000 / 7 = 4285.71 pence, rounded to 4286 pence = £42.86
        assertThat(caseData.getRentDetails().getCalculatedDailyCharge()).isEqualTo("4286");
        assertThat(caseData.getRentDetails().getFormattedCalculatedDailyCharge()).isEqualTo("£42.86");
    }

    @Test
    void shouldCalculateDailyRentForMonthlyFrequency() {
        // Given
        PCSCase caseData = PCSCase.builder()
                .rentDetails(RentDetails.builder()
                        .currentRent("30000") // £300.00 in pence
                        .frequency(RentPaymentFrequency.MONTHLY)
                        .build())
                .build();

        // When
        callMidEventHandler(caseData);

        // Then
        // 30000 / 30.44 = 985.55 pence, rounded to 986 pence = £9.86
        assertThat(caseData.getRentDetails().getCalculatedDailyCharge()).isEqualTo("986");
        assertThat(caseData.getRentDetails().getFormattedCalculatedDailyCharge()).isEqualTo("£9.86");
    }

    @Test
    void shouldCalculateDailyRentForFortnightlyFrequency() {
        // Given
        PCSCase caseData = PCSCase.builder()
                .rentDetails(RentDetails.builder()
                        .currentRent("30000") // £300.00 in pence
                        .frequency(RentPaymentFrequency.FORTNIGHTLY)
                        .build())
                .build();

        // When
        callMidEventHandler(caseData);

        // Then
        // 30000 / 14 = 2142.86 pence, rounded to 2143 pence = £21.43
        assertThat(caseData.getRentDetails().getCalculatedDailyCharge()).isEqualTo("2143");
        assertThat(caseData.getRentDetails().getFormattedCalculatedDailyCharge()).isEqualTo("£21.43");
    }

    @Test
    void shouldSetFormattedCurrencyWhenCalculatingDailyRent() {
        // Given - rent that results in whole pounds (no pence)
        PCSCase caseData = PCSCase.builder()
                .rentDetails(RentDetails.builder()
                        .currentRent("7000") // £70.00 in pence
                        .frequency(RentPaymentFrequency.WEEKLY)
                        .build())
                .build();

        // When
        callMidEventHandler(caseData);

        // Then
        // 7000 / 7 = 1000 pence = £10.00, formatted should strip trailing zeros to "£10"
        assertThat(caseData.getRentDetails().getCalculatedDailyCharge()).isEqualTo("1000");
        assertThat(caseData.getRentDetails().getFormattedCalculatedDailyCharge()).isEqualTo("£10");
    }

    @Test
    void shouldNotCalculateDailyRentWhenCurrentRentIsNull() {
        // Given
        PCSCase caseData = PCSCase.builder()
                .rentDetails(RentDetails.builder()
                        .currentRent(null)
                        .frequency(RentPaymentFrequency.WEEKLY)
                        .build())
                .build();

        // When
        callMidEventHandler(caseData);

        // Then
        assertThat(caseData.getRentDetails().getCalculatedDailyCharge()).isNull();
        assertThat(caseData.getRentDetails().getFormattedCalculatedDailyCharge()).isNull();
    }

    @Test
    void shouldNotCalculateDailyRentWhenCurrentRentIsEmpty() {
        // Given
        PCSCase caseData = PCSCase.builder()
                .rentDetails(RentDetails.builder()
                        .currentRent("")
                        .frequency(RentPaymentFrequency.WEEKLY)
                        .build())
                .build();

        // When
        callMidEventHandler(caseData);

        // Then
        assertThat(caseData.getRentDetails().getCalculatedDailyCharge()).isNull();
        assertThat(caseData.getRentDetails().getFormattedCalculatedDailyCharge()).isNull();
    }
}
