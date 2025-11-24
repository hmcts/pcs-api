package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
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

    @Test
    void shouldSetShowRentArrearsPageToNoForWeeklyFrequency() {
        // Given
        PCSCase caseData = PCSCase.builder()
                .rentFrequency(RentPaymentFrequency.WEEKLY)
                .currentRent("7000")
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
                .rentFrequency(RentPaymentFrequency.OTHER)
                .build();

        // When
        callMidEventHandler(caseData);

        // Then
        assertThat(caseData.getShowRentArrearsPage()).isEqualTo(YesOrNo.YES);
    }

    @Test
    void shouldCalculateDailyRentForFortnightlyFrequency() {
        // Given
        PCSCase caseData = PCSCase.builder()
                .currentRent("14000") // £140.00 in pence
                .rentFrequency(RentPaymentFrequency.FORTNIGHTLY)
                .build();

        // When
        callMidEventHandler(caseData);

        // Then
        assertThat(caseData.getCalculatedDailyRentChargeAmount()).isEqualTo("1000"); // £10.00 per day
        assertThat(caseData.getFormattedCalculatedDailyRentChargeAmount()).isEqualTo("£10.00");
    }

    @Test
    void shouldSetFormattedCurrencyWhenCalculatingDailyRent() {
        // Given
        PCSCase caseData = PCSCase.builder()
                .currentRent("7000") // £70.00 in pence
                .rentFrequency(RentPaymentFrequency.WEEKLY)
                .build();

        // When
        callMidEventHandler(caseData);

        // Then
        assertThat(caseData.getCalculatedDailyRentChargeAmount()).isEqualTo("1000"); // £10.00 per day in pence
        assertThat(caseData.getFormattedCalculatedDailyRentChargeAmount()).isEqualTo("£10.00");
    }

    @Test
    void shouldNotProcessWhenRentFrequencyIsNull() {
        // Given
        PCSCase caseData = PCSCase.builder()
                .currentRent("7000")
                .rentFrequency(null)
                .build();

        // When
        callMidEventHandler(caseData);

        // Then
        assertThat(caseData.getCalculatedDailyRentChargeAmount()).isNull();
        assertThat(caseData.getFormattedCalculatedDailyRentChargeAmount()).isNull();
        assertThat(caseData.getShowRentArrearsPage()).isNull();
    }

    @Test
    void shouldSetShowRentArrearsPageWhenCurrentRentIsEmpty() {
        // Given
        PCSCase caseData = PCSCase.builder()
                .currentRent("")
                .rentFrequency(RentPaymentFrequency.WEEKLY)
                .build();

        // When
        callMidEventHandler(caseData);

        // Then
        assertThat(caseData.getCalculatedDailyRentChargeAmount()).isNull();
        assertThat(caseData.getFormattedCalculatedDailyRentChargeAmount()).isNull();
        assertThat(caseData.getShowRentArrearsPage()).isEqualTo(YesOrNo.NO);
    }

    @Test
    void shouldSetShowRentArrearsPageToNoForMonthlyFrequency() {
        // Given
        PCSCase caseData = PCSCase.builder()
                .rentFrequency(RentPaymentFrequency.MONTHLY)
                .currentRent("30000")
                .build();

        // When
        callMidEventHandler(caseData);

        // Then
        assertThat(caseData.getShowRentArrearsPage()).isEqualTo(YesOrNo.NO);
    }
}
